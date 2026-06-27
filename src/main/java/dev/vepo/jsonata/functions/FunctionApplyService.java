package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.EvaluationEnvironment;
import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.FunctionData;
import dev.vepo.jsonata.functions.data.TailCallData;
import dev.vepo.jsonata.functions.signature.FunctionSignature;
import dev.vepo.jsonata.parser.BuiltInFunction;

/**
 * Unified function application for user-defined, partial, built-in, and registered functions.
 *
 * <p>All call sites — direct invocation, dynamic resolution, partial merge, and tail-call
 * trampoline — converge here so argument evaluation, signature validation, closure binding,
 * and tail-call looping share one path.
 *
 * @see FunctionValue
 * @see PartialApplication
 * @see RegisteredFunction
 */
public final class FunctionApplyService {

    private FunctionApplyService() {
    }

    /**
     * Applies a user-defined {@link FunctionValue} with evaluated arguments.
     *
     * @param fn           the function to invoke
     * @param original     root input document
     * @param current      current focus (or captured context when present)
     * @param argProviders unevaluated argument expressions
     * @return the function result, resolving any {@link TailCallData} thunks
     * @throws JSONataException when guardrails are violated
     */
    public static Data apply(FunctionValue fn, Data original, Data current, List<Mapping> argProviders) {
        EvaluationContext.checkGuardrails();
        var args = evaluateArgs(argProviders, original, current);
        var validated = validateArgs(fn, args, focus(fn, current));
        return invokeWithTrampoline(fn, original, focus(fn, current), validated);
    }

    /**
     * Merges call-site arguments into a {@link PartialApplication}, returning either a
     * further partial or the final result when all placeholders are filled.
     *
     * @param partial      the partially applied function
     * @param original     root input document
     * @param current      current focus
     * @param argProviders arguments for remaining {@code ?} placeholders
     * @return the result or a new partial wrapped in {@link FunctionData}
     * @throws JSONataException {@code T1008} when the partial target is not a function
     */
    public static Data applyPartial(PartialApplication partial, Data original, Data current,
                                    List<Mapping> argProviders) {
        EvaluationContext.checkGuardrails();
        if (partial.target() instanceof FunctionValue fn) {
            return mergePartial(fn, partial, original, current, argProviders);
        }
        if (partial.target() instanceof BuiltInFunctionWrapper wrapper) {
            return mergePartialBuiltIn(wrapper, partial, original, current, argProviders);
        }
        if (partial.target() instanceof RegisteredFunctionWrapper wrapper) {
            return mergePartialRegistered(wrapper, partial, original, current, argProviders);
        }
        throw new JSONataException("T1008", "Attempted to partial-apply a non-function");
    }

    /**
     * Resolves a dynamic call target (variable or expression) and applies arguments.
     *
     * @param targetResolver expression yielding the function value
     * @param original       root input document
     * @param current        current focus
     * @param argProviders   call-site arguments
     * @return the invocation result
     * @throws JSONataException {@code T1008} when the resolved value is not a function
     */
    public static Data applyDynamic(Mapping targetResolver, Data original, Data current,
                                    List<Mapping> argProviders) {
        EvaluationContext.checkGuardrails();
        var targetData = targetResolver.map(original, current);
        if (targetData instanceof FunctionData fd) {
            if (fd.isPartialApplication()) {
                return applyPartialWithArgs(fd.asPartialApplication(), original, current, argProviders);
            }
            return apply(fd.asFunctionValue(), original, current, argProviders);
        }
        throw new JSONataException("T1008", "Attempted to invoke a non-function");
    }

    /**
     * Applies a {@link DeclaredFunction} from block scope with optional captured context.
     *
     * @param fn           the declared function
     * @param original     root input document
     * @param current      current focus
     * @param argProviders call-site arguments
     * @param captured     optional captured context from a variable binding
     * @return the invocation result
     */
    public static Data applyDeclared(DeclaredFunction fn, Data original, Data current,
                                     List<Mapping> argProviders, Optional<Data> captured) {
        var fv = new FunctionValue(fn, captured, fn.signature());
        return apply(fv, original, current, argProviders);
    }

    private static Data applyPartialWithArgs(PartialApplication partial, Data original, Data current,
                                             List<Mapping> argProviders) {
        return applyPartial(partial, original, current, argProviders);
    }

    private static Data mergePartial(FunctionValue fn, PartialApplication partial, Data original, Data current,
                                     List<Mapping> argProviders) {
        var bound = new ArrayList<>(partial.boundArgs());
        fillPlaceholders(bound, argProviders, original, current);
        if (bound.stream().anyMatch(PartialApplication.PLACEHOLDER::equals)) {
            return FunctionValues.wrap(new PartialApplication(fn, bound, partial.signature()));
        }
        return invokeWithBoundArgs(fn, original, focus(fn, current), bound);
    }

    private static Data mergePartialBuiltIn(BuiltInFunctionWrapper wrapper, PartialApplication partial,
                                            Data original, Data current, List<Mapping> argProviders) {
        var bound = new ArrayList<>(partial.boundArgs());
        fillPlaceholders(bound, argProviders, original, current);
        if (bound.stream().anyMatch(PartialApplication.PLACEHOLDER::equals)) {
            return FunctionValues.wrap(new PartialApplication(wrapper, bound, partial.signature()));
        }
        return wrapper.invoke(bound.stream().map(o -> (Data) o).toList(), original, current);
    }

    private static Data mergePartialRegistered(RegisteredFunctionWrapper wrapper, PartialApplication partial,
                                               Data original, Data current, List<Mapping> argProviders) {
        var bound = new ArrayList<>(partial.boundArgs());
        fillPlaceholders(bound, argProviders, original, current);
        if (bound.stream().anyMatch(PartialApplication.PLACEHOLDER::equals)) {
            return FunctionValues.wrap(new PartialApplication(wrapper, bound, partial.signature()));
        }
        return wrapper.invoke(bound.stream().map(o -> (Data) o).toList(), original, current);
    }

    private static void fillPlaceholders(List<Object> bound, List<Mapping> argProviders, Data original, Data current) {
        var argIter = argProviders.iterator();
        for (int i = 0; i < bound.size() && argIter.hasNext(); i++) {
            if (PartialApplication.PLACEHOLDER.equals(bound.get(i))) {
                bound.set(i, evaluateArg(argIter.next(), original, current));
            }
        }
    }

    /**
     * Resolves a tail-call thunk produced by {@link TailCallFunctionCall}.
     *
     * @param thunk deferred call with target, arguments, and evaluation contexts
     * @return the result of applying the thunk
     * @throws JSONataException when the tail-call target is not a function
     */
    public static Data resolveTailCall(TailCallThunk thunk) {
        if (thunk.target() instanceof FunctionValue fv) {
            return apply(fv, thunk.original(), thunk.current(), thunk.args());
        }
        if (thunk.target() instanceof Mapping call) {
            return applyDynamic(call, thunk.original(), thunk.current(), thunk.args());
        }
        throw new JSONataException("Tail call target is not a function");
    }

    private static Data invokeWithTrampoline(FunctionValue fn, Data original, Data focus, List<Data> args) {
        Data result = invokeUserFunctionBody(fn, original, focus, args);
        while (result instanceof TailCallData tailCall) {
            EvaluationContext.checkGuardrails();
            result = tailCall.execute();
        }
        return result;
    }

    private static Data resolveThunk(TailCallThunk thunk) {
        return resolveTailCall(thunk);
    }

    private static Data invokeWithBoundArgs(FunctionValue fn, Data original, Data focus, List<Object> bound) {
        var args = bound.stream().map(o -> (Data) o).toList();
        return invokeUserFunctionBody(fn, original, focus, args);
    }

    private static Data invokeUserFunctionBody(FunctionValue fn, Data original, Data focus, List<Data> args) {
        var declared = fn.function();
        var frame = declared.closureContext().createChildFrame();
        var names = declared.parameterNames();
        PathBindings.pushScope();
        try {
            declared.closureContext().bindVariablesToPathBindings(original, focus);
            for (int i = 0; i < names.size(); i++) {
                var argValue = i < args.size() ? args.get(i) : Mapping.empty();
                final Data capturedArg = argValue;
                frame.defineVariable(names.get(i), (o, c) -> capturedArg);
                PathBindings.bind(names.get(i), capturedArg);
            }
            var result = declared.accept(original, focus, frame);
            return rebindReturnedFunction(result, frame, fn);
        } finally {
            PathBindings.popScope();
        }
    }

    private static Data rebindReturnedFunction(Data result, BlockContext frame, FunctionValue outer) {
        if (!(result instanceof FunctionData fd) || !fd.isFunctionValue()) {
            return result;
        }
        var inner = fd.asFunctionValue();
        var reboundFn = new DeclaredFunction(inner.function().parameterNames(), frame, inner.function().body(),
                                             inner.function().signature());
        var captured = inner.capturedContext().isPresent() ? inner.capturedContext() : outer.capturedContext();
        return FunctionValues.wrap(new FunctionValue(reboundFn, captured, inner.signature()));
    }

    private static List<Data> evaluateArgs(List<Mapping> providers, Data original, Data current) {
        var args = new ArrayList<Data>();
        for (var provider : providers) {
            if (provider instanceof PartialPlaceholder) {
                continue;
            }
            args.add(evaluateArg(provider, original, current));
        }
        return args;
    }

    private static Data evaluateArg(Mapping provider, Data original, Data current) {
        return provider.map(original, current);
    }

    private static List<Data> validateArgs(FunctionValue fn, List<Data> args, Data context) {
        var sig = fn.signature().or(() -> fn.function().signature());
        return sig.map(s -> s.validate(args, context)).orElse(args);
    }

    private static Data focus(FunctionValue fn, Data current) {
        return fn.capturedContext().orElse(current);
    }

    /**
     * Applies a parser-level built-in, supporting partial application via {@link PartialPlaceholder}.
     *
     * @param bif                the built-in definition
     * @param providers          unevaluated argument expressions (may include {@code ?})
     * @param declaredFunctions  block-scoped function declarations for compound built-ins
     * @param original           root input document
     * @param current            current focus
     * @return the built-in result or a partial application
     */
    public static Data applyBuiltIn(BuiltInFunction bif, List<Mapping> providers,
                                    List<DeclaredFunction> declaredFunctions, Data original, Data current) {
        if (hasPartialPlaceholder(providers)) {
            return partialApplyBuiltIn(bif, providers, declaredFunctions, original, current);
        }
        return bif.instantiate(providers, declaredFunctions).map(original, current);
    }

    private static boolean hasPartialPlaceholder(List<Mapping> providers) {
        return providers.stream().anyMatch(PartialPlaceholder.class::isInstance);
    }

    private static Data partialApplyBuiltIn(BuiltInFunction bif, List<Mapping> providers,
                                            List<DeclaredFunction> declaredFunctions, Data original, Data current) {
        var wrapper = new BuiltInFunctionWrapper(bif, declaredFunctions);
        var bound = buildBoundArgs(providers, original, current);
        if (bound.stream().anyMatch(PartialApplication.PLACEHOLDER::equals)) {
            return FunctionValues.wrap(new PartialApplication(wrapper, bound, Optional.empty()));
        }
        return wrapper.invoke(bound.stream().map(o -> (Data) o).toList(), original, current);
    }

    private static ArrayList<Object> buildBoundArgs(List<Mapping> providers, Data original, Data current) {
        var bound = new ArrayList<Object>();
        for (var provider : providers) {
            if (provider instanceof PartialPlaceholder) {
                bound.add(PartialApplication.PLACEHOLDER);
            } else {
                bound.add(evaluateArg(provider, original, current));
            }
        }
        return bound;
    }

    /**
     * Adapter that invokes a parser-level built-in with pre-evaluated arguments.
     *
     * @param bif                 the built-in definition
     * @param declaredFunctions   block-scoped declarations for compound built-ins
     */
    public record BuiltInFunctionWrapper(BuiltInFunction bif, List<DeclaredFunction> declaredFunctions) {

        /**
         * Invokes the built-in with evaluated argument values.
         *
         * @param args     evaluated arguments
         * @param original root input document
         * @param current  current focus
         * @return the built-in result
         */
        public Data invoke(List<Data> args, Data original, Data current) {
            var providers = args.stream().<Mapping>map(a -> (o, c) -> a).toList();
            return bif.instantiate(providers, declaredFunctions).map(original, current);
        }
    }

    /**
     * Applies an externally registered function from {@link EvaluationEnvironment}.
     *
     * @param name        the registered function name
     * @param providers   unevaluated argument expressions
     * @param environment the embedding environment holding registered implementations
     * @param original    root input document
     * @param current     current focus
     * @return the registered function result or a partial application
     * @throws JSONataException when the function is not registered
     */
    public static Data applyRegistered(String name, List<Mapping> providers, EvaluationEnvironment environment,
                                       Data original, Data current) {
        if (hasPartialPlaceholder(providers)) {
            var wrapper = new RegisteredFunctionWrapper(name, environment);
            var bound = buildBoundArgs(providers, original, current);
            if (bound.stream().anyMatch(PartialApplication.PLACEHOLDER::equals)) {
                return FunctionValues.wrap(new PartialApplication(wrapper, bound, Optional.empty()));
            }
            return wrapper.invoke(bound.stream().map(o -> (Data) o).toList(), original, current);
        }
        var impl = environment.functions().get(name);
        if (impl == null) {
            throw new JSONataException("Function not found: " + name);
        }
        var evaluatedArgs = evaluateArgs(providers, original, current);
        var argMappings = evaluatedArgs.stream().<Mapping>map(a -> (o, c) -> a).toList();
        return impl.apply(new EvaluationEnvironment.MappingCall(original, current, argMappings, List.of()));
    }

    /**
     * Adapter that invokes a registered function with pre-evaluated arguments.
     *
     * @param name        the registered function name
     * @param environment the embedding environment
     */
    public record RegisteredFunctionWrapper(String name, EvaluationEnvironment environment) {

        /**
         * Invokes the registered function with evaluated argument values.
         *
         * @param args     evaluated arguments
         * @param original root input document
         * @param current  current focus
         * @return the registered function result
         * @throws JSONataException when the function is not registered
         */
        public Data invoke(List<Data> args, Data original, Data current) {
            var impl = environment.functions().get(name);
            if (impl == null) {
                throw new JSONataException("Function not found: " + name);
            }
            var argMappings = args.stream().<Mapping>map(a -> (o, c) -> a).toList();
            return impl.apply(new EvaluationEnvironment.MappingCall(original, current, argMappings, List.of()));
        }
    }
}
