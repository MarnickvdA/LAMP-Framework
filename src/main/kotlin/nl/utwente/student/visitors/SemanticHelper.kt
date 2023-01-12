package nl.utwente.student.visitors

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit

object SemanticHelper {
    private fun <T: Expression> findAllInDeclarable(
        declarable: Declarable,
        typeCheck: (element: Expression) -> Boolean
    ): MutableList<T> {
        return when (declarable) {
            is Module -> findAllInModule(declarable, typeCheck)
            is Unit -> findAllInUnit(declarable, typeCheck)
            is Property -> findAllInProperty(declarable, typeCheck)
            else -> mutableListOf()
        }
    }

    fun <T: Expression> findAllInModule(module: Module?, typeCheck: (element: Expression) -> Boolean): MutableList<T> {
        if (module == null) return mutableListOf()
        return module.members.map { findAllInDeclarable<T>(it, typeCheck) }.flatten().toMutableList()
    }

    private fun <T: Expression> findAllInUnit(unit: Unit?, typeCheck: (element: Expression) -> Boolean): MutableList<T> {
        if (unit == null) return mutableListOf()

        return listOf(
            unit.parameters.map { findAllInProperty<T>(it, typeCheck) }.flatten(),
            findAllByExpressionType(unit.body, typeCheck)
        ).flatten().toMutableList()
    }

    private fun <T: Expression> findAllInProperty(
        property: Property?,
        typeCheck: (element: Expression) -> Boolean
    ): MutableList<T> {
        if (property == null) return mutableListOf()

        return listOf(
            findAllByExpressionType<T>(property.initializer, typeCheck),
            findAllInUnit(property.getter, typeCheck),
            findAllInUnit(property.setter, typeCheck)
        ).flatten().toMutableList()
    }

    fun <T: Expression> findAllByExpressionType(expression: Expression?, typeCheck: (element: Expression) -> Boolean): MutableList<T> {
        if (expression == null) return mutableListOf()

        val expressions = mutableListOf<T>()

        if (typeCheck(expression))
            expressions.add(expression as T)

        when (expression) {
            is LocalDeclaration -> return findAllInDeclarable(expression.declaration, typeCheck)
            is Assignment -> return listOf(
                findAllByExpressionType<T>(expression.value, typeCheck)
            ).flatten().toMutableList()

            is UnitCall -> {
                expressions.addAll(expression.arguments.map { findAllByExpressionType<T>(it, typeCheck) }.flatten())
            }

            is Lambda -> expressions.addAll(expression.parameters.map { findAllInProperty<T>(it, typeCheck) }.flatten())
            is SwitchCase -> expressions.addAll(findAllByExpressionType(expression.pattern, typeCheck))
            is Switch -> expressions.addAll(findAllByExpressionType(expression.subject, typeCheck))
            is Loop -> expressions.addAll(expression.evaluations.map { findAllByExpressionType<T>(it, typeCheck) }.flatten())
            is Conditional -> {
                expressions.addAll(findAllByExpressionType(expression.ifExpr, typeCheck))
                expression.elseIfExpr
                    ?.map { findAllByExpressionType<T>(it, typeCheck) }
                    ?.flatten()
                    ?.let { expressions.addAll(it) }
                expression.elseExpr
                    ?.let { expressions.addAll(findAllByExpressionType(it, typeCheck)) }
            }

            is LogicalSequence -> expression.operands
                ?.map { findAllByExpressionType<T>(it, typeCheck) }
                ?.flatten()
                ?.let { expressions.addAll(it) }
        }

        expression.innerScope
            ?.map { findAllByExpressionType<T>(it, typeCheck) }
            ?.flatten()
            ?.let { expressions.addAll(it) }

        return expressions
    }
}