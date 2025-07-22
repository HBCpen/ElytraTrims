package dev.kikugie.elytratrims.api.impl

import dev.kikugie.elytratrims.api.ETClientInitializer
import dev.kikugie.elytratrims.api.ETCommonInitializer

fun Iterable<ETClientInitializer>.sortClient() = sortedByDescending(ETClientInitializer::priority)
fun Iterable<ETCommonInitializer>.sortCommon() = sortedByDescending(ETCommonInitializer::priority)