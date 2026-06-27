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
 * Unified function application: user functions, partial applications, built-ins, registered functions.
 */
public final class FunctionApplyService {

    private FunctionApplyService() {
    }

    public static Data apply(FunctionValue fn, Data original, Data current, List<Mapping> argProviders) {
        EvaluationContext.checkGuardrails();
        var args = evaluateArgs(argProviders, original, current);
        var validated = validateArgs(fn, args, focus(fn, current));
        return invokeWithTrampoline(fn, original, focus(fn, current), validated);
    }

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

    public record BuiltInFunctionWrapper(BuiltInFunction bif, List<DeclaredFunction> declaredFunctions) {
        public Data invoke(List<Data> args, Data original, Data current) {
            var providers = args.stream().<Mapping>map(a -> (o, c) -> a).toList();
            return bif.instantiate(providers, declaredFunctions).map(original, current);
        }
    }

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

    public record RegisteredFunctionWrapper(String name, EvaluationEnvironment environment) {
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
