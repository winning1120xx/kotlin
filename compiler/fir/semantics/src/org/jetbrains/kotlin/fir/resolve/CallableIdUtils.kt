/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

// TODO: rename to `isFunctionOrKFunctionInvoke` when the compose builds will be stabilized, KT-67002
fun CallableId.isInvoke(): Boolean =
    isFunctionInvoke() || isKFunctionInvoke()

fun CallableId.isFunctionOrSuspendFunctionInvoke(): Boolean =
    isFunctionInvoke() || isSuspendFunctionInvoke()

fun CallableId.isSuspendFunctionInvoke(): Boolean =
    callableName.asString() == "invoke"
            && className?.asString()?.startsWith("SuspendFunction") == true
            && packageName == StandardClassIds.BASE_COROUTINES_PACKAGE

fun CallableId.isKSuspendFunctionInvoke(): Boolean =
    callableName.asString() == "invoke"
            && className?.asString()?.startsWith("KSuspendFunction") == true
            && packageName == StandardClassIds.BASE_REFLECT_PACKAGE

fun CallableId.isFunctionInvoke(): Boolean =
    callableName.asString() == "invoke"
            && className?.asString()?.startsWith("Function") == true
            && packageName == StandardClassIds.BASE_KOTLIN_PACKAGE

fun CallableId.isKFunctionInvoke(): Boolean =
    callableName.asString() == "invoke"
            && className?.asString()?.startsWith("KFunction") == true
            && packageName == StandardClassIds.BASE_REFLECT_PACKAGE

fun CallableId.isIteratorNext(): Boolean =
    callableName.asString() == "next" && className?.asString()?.endsWith("Iterator") == true
            && packageName == StandardClassIds.BASE_COLLECTIONS_PACKAGE

fun CallableId.isIteratorHasNext(): Boolean =
    callableName.asString() == "hasNext" && className?.asString()?.endsWith("Iterator") == true
            && packageName == StandardClassIds.BASE_COLLECTIONS_PACKAGE

fun CallableId.isIterator(): Boolean =
    callableName.asString() == "iterator" && packageName.asString() in arrayOf("kotlin", "kotlin.collections", "kotlin.ranges")

fun FirAnnotation.fqName(session: FirSession): FqName? {
    val symbol = annotationTypeRef.coneTypeSafe<ConeSimpleKotlinType>()?.toRegularClassSymbol(session) ?: return null
    return symbol.classId.asSingleFqName()
}

fun ConeClassLikeLookupTag.isRealOwnerOf(declarationSymbol: FirCallableSymbol<*>): Boolean =
    this == declarationSymbol.dispatchReceiverClassLookupTagOrNull()
