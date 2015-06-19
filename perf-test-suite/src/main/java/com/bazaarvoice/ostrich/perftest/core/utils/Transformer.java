package com.bazaarvoice.ostrich.perftest.core.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Transformer {

    // i.e. Function<Integer, Number> typeCastedTransformer = getNoop();
    public static <S extends D, D> Function<S, D> getNoop() {
        return new Function<S, D>() {
            @Override
            public D apply(S source) {
                return source;
            }
        };
    }

    // i.e. Function<Object, String> toStringTransformer = getToString();
    public static <S> Function<S, String> getToString() {
        return new Function<S, String>() {
            @Override
            public String apply(S source) {
                return String.valueOf(source);
            }
        };
    }

    public static<KS, KD, VS, VD> Map<KD, VD> transform(Map<KS, VS> sourceMap, Function<KS, KD> keyTransformer,
                                  Function<VS, VD> valueTransformer) {
        return transform(sourceMap, Maps.<KD, VD>newHashMap(), keyTransformer, valueTransformer);
    }

    public static <KS, KD, VS, VD> Map<KD, VD> transform(Map<KS, VS> sourceMap, Map<KD, VD> destinationMap,
                                   Function<KS, KD> keyTransformer, Function<VS, VD> valueTransformer) {
        checkNotNull(sourceMap);
        checkNotNull(destinationMap);
        checkNotNull(keyTransformer);
        checkNotNull(valueTransformer);
        for (Map.Entry<KS, VS> entry : sourceMap.entrySet()) {
            destinationMap.put(keyTransformer.apply(entry.getKey()), valueTransformer.apply(entry.getValue()));
        }
        return destinationMap;
    }

    public static <S, D, LS extends List<S>> List<D> transform(LS sourceList, Function<S, D> valueTransformer) {
        return transform(sourceList, Lists.<D>newArrayList(), valueTransformer);
    }

    public static <S, D, LS extends List<S>, LD extends List<D>> LD transform(LS sourceList, LD destinationList,
                                                                 Function<S, D> valueTransformer) {
        checkNotNull(sourceList);
        checkNotNull(destinationList);
        checkNotNull(valueTransformer);
        for (S entry : sourceList) {
            destinationList.add(valueTransformer.apply(entry));
        }
        return destinationList;
    }

    public static <S, D, SS extends Set<S>> Set<D> transform(SS sourceSet, Function<S, D> valueTransformer) {
        return transform(sourceSet, Sets.<D>newHashSet(), valueTransformer);
    }

    public static <S, D, SS extends Set<S>, SD extends Set<D>> SD transform(SS sourceSet, SD destinationSet,
                                                               Function<S, D> valueTransformer) {
        checkNotNull(sourceSet);
        checkNotNull(destinationSet);
        checkNotNull(valueTransformer);
        for (S entry : sourceSet) {
            destinationSet.add(valueTransformer.apply(entry));
        }
        return destinationSet;
    }
}
