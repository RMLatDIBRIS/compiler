package compiler.rml.parser

import compiler.rml.ast.*
import rml.parser.RMLBaseVisitor
import rml.parser.RMLParser.*
import java.lang.RuntimeException

// this file implements the conversion from the ANTLR parse tree to the RML AST
// grammar rules with multiple productions are converted with (singleton) visitors, otherwise single functions are used

fun buildSpecification(ctx: SpecificationContext) = Specification(
        ctx.eventTypeDeclaration().map { it.accept(EventTypeDeclarationBuilder) },
        ctx.equation().map(::buildEquation),
        Identifier("Main")
)

fun buildEquation(ctx: EquationContext) = Equation(
        ctx.expId().UPPERCASE_ID().text,
        ctx.expVar().map { Identifier(it.text) },
        ctx.exp().accept(ExpressionBuilder)
)

object EventTypeDeclarationBuilder: RMLBaseVisitor<EventTypeDeclaration>() {
    override fun visitDerivedEvtypeDecl(ctx: DerivedEvtypeDeclContext?) =
            DerivedEventTypeDeclaration(
                    buildEventType(ctx!!.evtype(0)),
                    ctx.evtype().drop(1).map(::buildEventType),
                    ctx.NOT() != null,
                    ctx.dataExp()?.accept(DataExpressionBuilder)
            )

    override fun visitDirectEvtypeDecl(ctx: DirectEvtypeDeclContext?) =
            DirectEventTypeDeclaration(
                    buildEventType(ctx!!.evtype()),
                    ctx.eventExp().accept(EventExpressionBuilder),
                    ctx.NOT() != null,
                    ctx.dataExp()?.accept(DataExpressionBuilder)
            )
}

object EventTypeParameterBuilder: RMLBaseVisitor<EventType.Parameter>() {
    override fun visitEvtypeVarParam(ctx: EvtypeVarParamContext?) =
            EventType.Parameter.Variable(ctx!!.evtypeVar().LOWERCASE_ID().text)

    override fun visitEvtypeEventExpParam(ctx: EvtypeEventExpParamContext?) =
            EventType.Parameter.EventExpression(ctx!!.eventExp().accept(EventExpressionBuilder))

    override fun visitEvtypeDataExpParam(ctx: EvtypeDataExpParamContext?) =
            EventType.Parameter.DataExpression(ctx!!.dataExp().accept(DataExpressionBuilder))
}

object EventExpressionBuilder: RMLBaseVisitor<EventExpression>() {
    override fun visitPatternEventExp(ctx: PatternEventExpContext?) =
            PatternEventExpression(ctx!!.eventExp(0).accept(this), ctx.eventExp(1).accept(this))

    override fun visitObjectEventExp(ctx: ObjectEventExpContext?) =
            ObjectEventExpression(ctx!!.field().map { buildField(it) })

    override fun visitListEventExp(ctx: ListEventExpContext?) =
            ListEventExpression(ctx!!.eventExp().map { it.accept(this) }, ctx.ELLIPSIS() != null)

    override fun visitParenEventExp(ctx: ParenEventExpContext?): EventExpression = ctx!!.eventExp().accept(this)

    override fun visitStringEventExp(ctx: StringEventExpContext?) =
            StringEventExpression(ctx!!.STRING().text.removeSurrounding("'"))

    override fun visitIntEventExp(ctx: IntEventExpContext?) =
            IntEventExpression(ctx!!.INT().text.toInt())

    override fun visitFloatEventExp(ctx: FloatEventExpContext?) =
            FloatEventExpression(ctx!!.FLOAT().text.toDouble())

    override fun visitBoolEventExp(ctx: BoolEventExpContext?) =
            BoolEventExpression(ctx!!.BOOLEAN().text!!.toBoolean())

    override fun visitVarEventExp(ctx: VarEventExpContext?) =
            VariableEventExpression(ctx!!.evtypeVar().LOWERCASE_ID().text)

    override fun visitIgnoredEventExp(ctx: IgnoredEventExpContext?) = IgnoredEventExpression
}

object ExpressionBuilder: RMLBaseVisitor<Expression>() {
    override fun visitStarExp(ctx: StarExpContext?) =
            StarExpression(ctx!!.exp().accept(this))

    override fun visitPlusExp(ctx: PlusExpContext?) =
            PlusExpression(ctx!!.exp().accept(this))

    override fun visitOptionalExp(ctx: OptionalExpContext?) =
            OptionalExpression(ctx!!.exp().accept(this))

    override fun visitClosureExp(ctx: ClosureExpContext?) =
            PrefixClosureExpression(ctx!!.exp().accept(this))

    override fun visitCatExp(ctx: CatExpContext?) = ConcatExpression(
            ctx!!.exp(0).accept(this),
            ctx.exp(1).accept(this)
    )

    override fun visitAndExp(ctx: AndExpContext?) = AndExpression(
            ctx!!.exp(0).accept(this),
            ctx.exp(1).accept(this)
    )

    override fun visitOrExp(ctx: OrExpContext?) = OrExpression(
            ctx!!.exp(0).accept(this),
            ctx.exp(1).accept(this)
    )

    override fun visitShufExp(ctx: ShufExpContext?) = ShuffleExpression(
            ctx!!.exp(0).accept(this),
            ctx.exp(1).accept(this)
    )

    override fun visitFilterExp(ctx: FilterExpContext?) = FilterExpression(
            buildEventType(ctx!!.evtype()),
            ctx.exp(0).accept(this),
            if (ctx.exp().size > 1) ctx.exp(1).accept(this) else null
    )

    override fun visitEmptyExp(ctx: EmptyExpContext?) = EmptyExpression
    override fun visitNoneExp(ctx: NoneExpContext?) = NoneExpression
    override fun visitAllExp(ctx: AllExpContext?) = AllExpression

    override fun visitBlockExp(ctx: BlockExpContext?) = BlockExpression(
            ctx!!.evtypeVar().map { Identifier(it.text) },
            ctx.exp().accept(this)
    )

    override fun visitIfElseExp(ctx: IfElseExpContext?) = IfElseExpression(
            ctx!!.dataExp().accept(DataExpressionBuilder),
            ctx.exp(0).accept(this),
            ctx.exp(1).accept(this)
    )

    override fun visitVarExp(ctx: VarExpContext?) = VariableExpression(
            ctx!!.expId().UPPERCASE_ID().text,
            ctx.dataExp().map { it.accept(DataExpressionBuilder) }
    )

    override fun visitEvtypeExp(ctx: EvtypeExpContext?) =
            EventTypeExpression(buildEventType(ctx!!.evtype()))

    override fun visitParenExp(ctx: ParenExpContext?): Expression = ctx!!.exp().accept(this)
}

object DataExpressionBuilder: RMLBaseVisitor<DataExpression>() {
    // factorize these three functions is just not worth it
    override fun visitBoolDataExp(ctx: BoolDataExpContext?) =
            BoolDataExpression(ctx!!.BOOLEAN().text!!.toBoolean())
    override fun visitIntDataExp(ctx: IntDataExpContext?) =
            IntDataExpression(ctx!!.INT().text!!.toInt())
    override fun visitFloatDataExp(ctx: FloatDataExpContext?) =
            FloatDataExpression(ctx!!.FLOAT().text!!.toDouble())

    override fun visitVarDataExp(ctx: VarDataExpContext?) =
            VariableDataExpression(ctx!!.evtypeVar().LOWERCASE_ID().text)

    override fun visitSumDataExp(ctx: SumDataExpContext?) =
            SumDataExpression(ctx!!.dataExp(0).accept(this), ctx.dataExp(1).accept(this))

    override fun visitSubDataExp(ctx: SubDataExpContext?) =
            SubDataExpression(ctx!!.dataExp(0).accept(this), ctx.dataExp(1).accept(this))

    override fun visitLessThanDataExp(ctx: LessThanDataExpContext?) =
            LessThanDataExpression(ctx!!.dataExp(0).accept(this), ctx.dataExp(1).accept(this))

    override fun visitLessThanEqualToDataExp(ctx: LessThanEqualToDataExpContext?) =
            LessThanEqualDataExpression(ctx!!.dataExp(0).accept(this), ctx.dataExp(1).accept(this))

    override fun visitGreaterThanDataExp(ctx: GreaterThanDataExpContext?) =
            GreaterThanDataExpression(ctx!!.dataExp(0).accept(this), ctx.dataExp(1).accept(this))

    override fun visitGreaterThanEqualToDataExp(ctx: GreaterThanEqualToDataExpContext?) =
            GreaterThanEqualDataExpression(ctx!!.dataExp(0).accept(this), ctx.dataExp(1).accept(this))

    override fun visitAndDataExp(ctx: AndDataExpContext?) =
            AndDataExpression(ctx!!.dataExp(0).accept(this), ctx.dataExp(1).accept(this))

    override fun visitOrDataExp(ctx: OrDataExpContext?) =
            OrDataExpression(ctx!!.dataExp(0).accept(this), ctx.dataExp(1).accept(this))
}

fun buildEventType(ctx: EvtypeContext) = EventType(
        ctx.evtypeId().LOWERCASE_ID().text,
        ctx.evtypeParam().map { it.accept(EventTypeParameterBuilder) }
)

fun buildField(ctx: FieldContext) = ObjectEventExpression.Field(
        ctx.fieldKey().LOWERCASE_ID().text,
        ctx.eventExp().accept(EventExpressionBuilder)
)