package dev.vepo.jsonata.parser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.builtin.Abs;
import dev.vepo.jsonata.functions.builtin.Append;
import dev.vepo.jsonata.functions.builtin.Assert;
import dev.vepo.jsonata.functions.builtin.Average;
import dev.vepo.jsonata.functions.builtin.Base64Decode;
import dev.vepo.jsonata.functions.builtin.Base64Encode;
import dev.vepo.jsonata.functions.builtin.BuiltInSupplier;
import dev.vepo.jsonata.functions.builtin.Ceil;
import dev.vepo.jsonata.functions.builtin.Contains;
import dev.vepo.jsonata.functions.builtin.Count;
import dev.vepo.jsonata.functions.builtin.DecodeUrl;
import dev.vepo.jsonata.functions.builtin.DecodeUrlComponent;
import dev.vepo.jsonata.functions.builtin.Distinct;
import dev.vepo.jsonata.functions.builtin.Each;
import dev.vepo.jsonata.functions.builtin.EncodeUrl;
import dev.vepo.jsonata.functions.builtin.EncodeUrlComponent;
import dev.vepo.jsonata.functions.builtin.Error;
import dev.vepo.jsonata.functions.builtin.Eval;
import dev.vepo.jsonata.functions.builtin.Exists;
import dev.vepo.jsonata.functions.builtin.Filter;
import dev.vepo.jsonata.functions.builtin.Floor;
import dev.vepo.jsonata.functions.builtin.FnBoolean;
import dev.vepo.jsonata.functions.builtin.FnNot;
import dev.vepo.jsonata.functions.builtin.FnNumber;
import dev.vepo.jsonata.functions.builtin.FnString;
import dev.vepo.jsonata.functions.builtin.FormatBase;
import dev.vepo.jsonata.functions.builtin.FormatInteger;
import dev.vepo.jsonata.functions.builtin.FormatNumber;
import dev.vepo.jsonata.functions.builtin.FromMillis;
import dev.vepo.jsonata.functions.builtin.Join;
import dev.vepo.jsonata.functions.builtin.Keys;
import dev.vepo.jsonata.functions.builtin.Length;
import dev.vepo.jsonata.functions.builtin.Lookup;
import dev.vepo.jsonata.functions.builtin.Lowecase;
import dev.vepo.jsonata.functions.builtin.MapFn;
import dev.vepo.jsonata.functions.builtin.Match;
import dev.vepo.jsonata.functions.builtin.Max;
import dev.vepo.jsonata.functions.builtin.Merge;
import dev.vepo.jsonata.functions.builtin.Millis;
import dev.vepo.jsonata.functions.builtin.Min;
import dev.vepo.jsonata.functions.builtin.Now;
import dev.vepo.jsonata.functions.builtin.Pad;
import dev.vepo.jsonata.functions.builtin.ParseInteger;
import dev.vepo.jsonata.functions.builtin.Power;
import dev.vepo.jsonata.functions.builtin.Random;
import dev.vepo.jsonata.functions.builtin.Reduce;
import dev.vepo.jsonata.functions.builtin.Replace;
import dev.vepo.jsonata.functions.builtin.Reverse;
import dev.vepo.jsonata.functions.builtin.Round;
import dev.vepo.jsonata.functions.builtin.Shuffle;
import dev.vepo.jsonata.functions.builtin.Sift;
import dev.vepo.jsonata.functions.builtin.Single;
import dev.vepo.jsonata.functions.builtin.Sort;
import dev.vepo.jsonata.functions.builtin.Spread;
import dev.vepo.jsonata.functions.builtin.Split;
import dev.vepo.jsonata.functions.builtin.Sqrt;
import dev.vepo.jsonata.functions.builtin.Substring;
import dev.vepo.jsonata.functions.builtin.SubstringAfter;
import dev.vepo.jsonata.functions.builtin.SubstringBefore;
import dev.vepo.jsonata.functions.builtin.Sum;
import dev.vepo.jsonata.functions.builtin.ToMillis;
import dev.vepo.jsonata.functions.builtin.Trim;
import dev.vepo.jsonata.functions.builtin.TypeOf;
import dev.vepo.jsonata.functions.builtin.Uppercase;
import dev.vepo.jsonata.functions.builtin.Zip;

public enum BuiltInFunction {
    SORT("$sort", Sort::new),
    SUM("$sum", Sum::new),
    STRING("$string", FnString::new),
    LENGTH("$length", Length::new),
    SUBSTRING("$substring", Substring::new),
    SUBSTRING_BEFORE("$substringBefore", SubstringBefore::new),
    SUBSTRING_AFTER("$substringAfter", SubstringAfter::new),
    LOWERCASE("$lowercase", Lowecase::new),
    UPPERCASE("$uppercase", Uppercase::new),
    TRIM("$trim", Trim::new),
    PAD("$pad", Pad::new),
    CONTAINS("$contains", Contains::new),
    SPLIT("$split", Split::new),
    MAX("$max", Max::new),
    MIN("$min", Min::new),
    AVERAGE("$average", Average::new),
    COUNT("$count", Count::new),
    JOIN("$join", Join::new),
    BOOLEAN("$boolean", FnBoolean::new),
    NOT("$not", FnNot::new),
    EXISTS("$exists", Exists::new),
    NUMBER("$number", FnNumber::new),
    ABS("$abs", Abs::new),
    FLOOR("$floor", Floor::new),
    CEIL("$ceil", Ceil::new),
    ROUND("$round", Round::new),
    POWER("$power", Power::new),
    SQRT("$sqrt", Sqrt::new),
    RANDOM("$random", Random::new),
    FORMAT_NUMBER("$formatNumber", FormatNumber::new),
    FORMAT_BASE("$formatBase", FormatBase::new),
    FORMAT_INTEGER("$formatInteger", FormatInteger::new),
    PARSE_INTEGER("$parseInteger", ParseInteger::new),
    APPEND("$append", Append::new),
    REVERSE("$reverse", Reverse::new),
    SHUFFLE("$shuffle", Shuffle::new),
    DISTINCT("$distinct", Distinct::new),
    ZIP("$zip", Zip::new),
    KEYS("$keys", Keys::new),
    LOOKUP("$lookup", Lookup::new),
    SPREAD("$spread", Spread::new),
    MERGE("$merge", Merge::new),
    SIFT("$sift", Sift::new),
    EACH("$each", Each::new),
    ERROR("$error", Error::new),
    ASSERT("$assert", Assert::new),
    TYPE("$type", TypeOf::new),
    MAP("$map", MapFn::new),
    FILTER("$filter", Filter::new),
    SINGLE("$single", Single::new),
    REDUCE("$reduce", Reduce::new),
    MATCH("$match", Match::new),
    REPLACE("$replace", Replace::new),
    EVAL("$eval", Eval::new),
    NOW("$now", Now::new),
    MILLIS("$millis", Millis::new),
    FROM_MILLIS("$fromMillis", FromMillis::new),
    TO_MILLIS("$toMillis", ToMillis::new),
    BASE64_ENCODE("$base64encode", Base64Encode::new),
    BASE64_DECODE("$base64decode", Base64Decode::new),
    ENCODE_URL("$encodeUrl", EncodeUrl::new),
    ENCODE_URL_COMPONENT("$encodeUrlComponent", EncodeUrlComponent::new),
    DECODE_URL("$decodeUrl", DecodeUrl::new),
    DECODE_URL_COMPONENT("$decodeUrlComponent", DecodeUrlComponent::new);

    public static Optional<BuiltInFunction> get(String name) {
        return Stream.of(values())
                     .filter(n -> n.name.compareToIgnoreCase(name) == 0)
                     .findAny();
    }

    private final String name;
    private final BuiltInSupplier supplier;

    BuiltInFunction(String name, BuiltInSupplier supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    Mapping instantiate(List<Mapping> valueProviders, List<DeclaredFunction> functions) {
        return supplier.instantiate(valueProviders, functions);
    }
}
