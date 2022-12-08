package nl.utwente.student.transformers

import nl.utwente.student.model.Module
import nl.utwente.student.visitor.java.JavaParser
import nl.utwente.student.visitor.java.JavaParserBaseVisitor
import org.antlr.v4.runtime.ParserRuleContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class Java2XMLTransformer(val file: File): JavaParserBaseVisitor<Node?>() {
    private lateinit var currentDoc: Document
    private var moduleName = "?"
    private var packageName = "?"
    val documents = mutableListOf<Module>()

    private fun getModuleMetadata(ctx: JavaParser.TypeDeclarationContext): Element {
        val metadata = currentDoc.createElement("Metadata")

        val fileElement = currentDoc.createElement("File")
        fileElement.setAttribute("path", file.absolutePath)
        fileElement.setAttribute("name", file.name)

        metadata.appendChild(fileElement)
        metadata.appendChild(getSourceMetadata(ctx, "Location"))
        return metadata
    }

    /**
     * Metadata about the source code that needs to be added to the module, module members, statements and expressions.
     */
    private fun getSourceMetadata(ctx: ParserRuleContext, elementName: String = "Metadata"): Element {
        val sourceMetadata = currentDoc.createElement(elementName)
        sourceMetadata.setAttribute("startLine", ctx.start.line.toString())
        sourceMetadata.setAttribute("endLine", ctx.stop.line.toString())
        sourceMetadata.setAttribute("startOffset", ctx.start.charPositionInLine.toString())
        sourceMetadata.setAttribute("endOffset", ctx.stop.charPositionInLine.toString())

        return sourceMetadata
    }

    override fun visitCompilationUnit(ctx: JavaParser.CompilationUnitContext?): Node? {
        return super.visitCompilationUnit(ctx)
    }

    override fun visitPackageDeclaration(ctx: JavaParser.PackageDeclarationContext?): Node? {
        packageName = ctx?.qualifiedName()?.identifier()?.joinToString(".") { it.text }.toString()
        return null // Prevent these contents from being visited.
    }

    override fun visitImportDeclaration(ctx: JavaParser.ImportDeclarationContext?): Node? {
        return null // Prevent these contents from being visited.
    }

    override fun visitTypeDeclaration(ctx: JavaParser.TypeDeclarationContext?): Node? {
        print("Visiting '${file.name}', lines=[${ctx!!.start.line}:${ctx.stop.line}]")

        val factory = DocumentBuilderFactory.newInstance()
        val docBuilder = factory.newDocumentBuilder()

        currentDoc = docBuilder.newDocument()
        val module = currentDoc.createElement("Module")
        module.setAttribute("xmlns", "https://www.utwente.nl")
        module.appendChild(this.getModuleMetadata(ctx))

        currentDoc.appendChild(module)

        /*
        Create ModuleHeader and its contents
         */
        val moduleHeader = currentDoc.createElement("ModuleHeader")
        module.appendChild(moduleHeader)

        val moduleModifierList = currentDoc.createElement("ModifierList")
        moduleHeader.appendChild(moduleModifierList)

        ctx.classOrInterfaceModifier()?.forEach {
            val modifierItem = this.visitClassOrInterfaceModifier(it)
            moduleModifierList.appendChild(modifierItem)
        }

        /*
        Create ModuleBody and its contents, depending on which declaration isn't null.
         */
        ctx.classDeclaration()?.let { module.appendChild(this.visitClassDeclaration(it))}
        ctx.enumDeclaration()?.let { module.appendChild(this.visitEnumDeclaration(it))}
        ctx.interfaceDeclaration()?.let { module.appendChild(this.visitInterfaceDeclaration(it))}
        ctx.recordDeclaration()?.let { module.appendChild(this.visitRecordDeclaration(it))}

        documents.add(Module(moduleName, packageName, currentDoc, file))
        println(". Done!")
        return module
    }

    override fun visitModifier(ctx: JavaParser.ModifierContext?): Node? {
        return super.visitModifier(ctx)
    }

    override fun visitClassOrInterfaceModifier(ctx: JavaParser.ClassOrInterfaceModifierContext?): Node? {
        val item = currentDoc.createElement("Item")
        var value = ""

        ctx?.annotation()?.let {
            this.visitAnnotation(it)
        }

        ctx?.PUBLIC()?.let {value = "public"}
        ctx?.PROTECTED()?.let {value = "protected"}
        ctx?.PRIVATE()?.let {value = "private"}
        ctx?.STATIC()?.let {value = "static"}
        ctx?.ABSTRACT()?.let {value = "abstract"}
        ctx?.FINAL()?.let {value = "final"}
        ctx?.SEALED()?.let {value = "sealed"}
        ctx?.NON_SEALED()?.let {value = "open"}

        item.appendChild(currentDoc.createTextNode(value))

        return item
    }

    override fun visitVariableModifier(ctx: JavaParser.VariableModifierContext?): Node? {
        return super.visitVariableModifier(ctx)
    }

    override fun visitClassDeclaration(ctx: JavaParser.ClassDeclarationContext?): Node? {
        val moduleHeader = currentDoc.getElementsByTagName("ModuleHeader").item(0) as Element
        moduleName = ctx?.identifier()?.text.toString()
        moduleHeader.setAttribute("identifier", moduleName)
        moduleHeader.setAttribute("moduleType", "class")

        if (ctx?.EXTENDS() != null) {
            val extends = currentDoc.createElement("Extends")
            extends.appendChild(visitTypeType(ctx.typeType()))

            moduleHeader.appendChild(extends)
        }

        if (ctx?.IMPLEMENTS() != null) {
            val implementsList = currentDoc.createElement("Implements")
            ctx.typeList()?.get(0)?.typeType()?.forEach {
                val name = currentDoc.createElement("Name")
                name.appendChild(this.visitTypeType(it))

                implementsList.appendChild(name)
            }

            moduleHeader.appendChild(implementsList)
        }

        val moduleBody = currentDoc.createElement("ModuleBody")

        this.visitClassBody(ctx?.classBody())

        return moduleBody
    }

    override fun visitTypeParameters(ctx: JavaParser.TypeParametersContext?): Node? {
        return super.visitTypeParameters(ctx)
    }

    override fun visitTypeParameter(ctx: JavaParser.TypeParameterContext?): Node? {
        return super.visitTypeParameter(ctx)
    }

    override fun visitTypeBound(ctx: JavaParser.TypeBoundContext?): Node? {
        return super.visitTypeBound(ctx)
    }

    override fun visitEnumDeclaration(ctx: JavaParser.EnumDeclarationContext?): Node? {
        return super.visitEnumDeclaration(ctx)
    }

    override fun visitEnumConstants(ctx: JavaParser.EnumConstantsContext?): Node? {
        return super.visitEnumConstants(ctx)
    }

    override fun visitEnumConstant(ctx: JavaParser.EnumConstantContext?): Node? {
        return super.visitEnumConstant(ctx)
    }

    override fun visitEnumBodyDeclarations(ctx: JavaParser.EnumBodyDeclarationsContext?): Node? {
        return super.visitEnumBodyDeclarations(ctx)
    }

    override fun visitInterfaceDeclaration(ctx: JavaParser.InterfaceDeclarationContext?): Node? {
        return super.visitInterfaceDeclaration(ctx)
    }

    override fun visitClassBody(ctx: JavaParser.ClassBodyContext?): Node? {
        return super.visitClassBody(ctx)
    }

    override fun visitInterfaceBody(ctx: JavaParser.InterfaceBodyContext?): Node? {
        return super.visitInterfaceBody(ctx)
    }

    override fun visitClassBodyDeclaration(ctx: JavaParser.ClassBodyDeclarationContext?): Node? {
        return super.visitClassBodyDeclaration(ctx)
    }

    override fun visitMemberDeclaration(ctx: JavaParser.MemberDeclarationContext?): Node? {
        return super.visitMemberDeclaration(ctx)
    }

    override fun visitMethodDeclaration(ctx: JavaParser.MethodDeclarationContext?): Node? {
        return super.visitMethodDeclaration(ctx)
    }

    override fun visitMethodBody(ctx: JavaParser.MethodBodyContext?): Node? {
        return super.visitMethodBody(ctx)
    }

    override fun visitTypeTypeOrVoid(ctx: JavaParser.TypeTypeOrVoidContext?): Node? {
        return super.visitTypeTypeOrVoid(ctx)
    }

    override fun visitGenericMethodDeclaration(ctx: JavaParser.GenericMethodDeclarationContext?): Node? {
        return super.visitGenericMethodDeclaration(ctx)
    }

    override fun visitGenericConstructorDeclaration(ctx: JavaParser.GenericConstructorDeclarationContext?): Node? {
        return super.visitGenericConstructorDeclaration(ctx)
    }

    override fun visitConstructorDeclaration(ctx: JavaParser.ConstructorDeclarationContext?): Node? {
        return super.visitConstructorDeclaration(ctx)
    }

    override fun visitFieldDeclaration(ctx: JavaParser.FieldDeclarationContext?): Node? {
        return super.visitFieldDeclaration(ctx)
    }

    override fun visitInterfaceBodyDeclaration(ctx: JavaParser.InterfaceBodyDeclarationContext?): Node? {
        return super.visitInterfaceBodyDeclaration(ctx)
    }

    override fun visitInterfaceMemberDeclaration(ctx: JavaParser.InterfaceMemberDeclarationContext?): Node? {
        return super.visitInterfaceMemberDeclaration(ctx)
    }

    override fun visitConstDeclaration(ctx: JavaParser.ConstDeclarationContext?): Node? {
        return super.visitConstDeclaration(ctx)
    }

    override fun visitConstantDeclarator(ctx: JavaParser.ConstantDeclaratorContext?): Node? {
        return super.visitConstantDeclarator(ctx)
    }

    override fun visitInterfaceMethodDeclaration(ctx: JavaParser.InterfaceMethodDeclarationContext?): Node? {
        return super.visitInterfaceMethodDeclaration(ctx)
    }

    override fun visitInterfaceMethodModifier(ctx: JavaParser.InterfaceMethodModifierContext?): Node? {
        return super.visitInterfaceMethodModifier(ctx)
    }

    override fun visitGenericInterfaceMethodDeclaration(ctx: JavaParser.GenericInterfaceMethodDeclarationContext?): Node? {
        return super.visitGenericInterfaceMethodDeclaration(ctx)
    }

    override fun visitInterfaceCommonBodyDeclaration(ctx: JavaParser.InterfaceCommonBodyDeclarationContext?): Node? {
        return super.visitInterfaceCommonBodyDeclaration(ctx)
    }

    override fun visitVariableDeclarators(ctx: JavaParser.VariableDeclaratorsContext?): Node? {
        return super.visitVariableDeclarators(ctx)
    }

    override fun visitVariableDeclarator(ctx: JavaParser.VariableDeclaratorContext?): Node? {
        return super.visitVariableDeclarator(ctx)
    }

    override fun visitVariableDeclaratorId(ctx: JavaParser.VariableDeclaratorIdContext?): Node? {
        return super.visitVariableDeclaratorId(ctx)
    }

    override fun visitVariableInitializer(ctx: JavaParser.VariableInitializerContext?): Node? {
        return super.visitVariableInitializer(ctx)
    }

    override fun visitArrayInitializer(ctx: JavaParser.ArrayInitializerContext?): Node? {
        return super.visitArrayInitializer(ctx)
    }

    override fun visitClassOrInterfaceType(ctx: JavaParser.ClassOrInterfaceTypeContext?): Node? {
        var typeName = ""

        ctx?.identifier()?.forEach { typeName += (visitIdentifier(it) as Text).wholeText + "." }
        ctx?.typeIdentifier()?.let { typeName += (visitTypeIdentifier(it) as Text).wholeText }

        return currentDoc.createTextNode(typeName)
    }

    override fun visitTypeArgument(ctx: JavaParser.TypeArgumentContext?): Node? {
        return this.visitTypeType(ctx?.typeType())
    }

    override fun visitQualifiedNameList(ctx: JavaParser.QualifiedNameListContext?): Node? {
        return super.visitQualifiedNameList(ctx)
    }

    override fun visitFormalParameters(ctx: JavaParser.FormalParametersContext?): Node? {
        return super.visitFormalParameters(ctx)
    }

    override fun visitReceiverParameter(ctx: JavaParser.ReceiverParameterContext?): Node? {
        return super.visitReceiverParameter(ctx)
    }

    override fun visitFormalParameterList(ctx: JavaParser.FormalParameterListContext?): Node? {
        return super.visitFormalParameterList(ctx)
    }

    override fun visitFormalParameter(ctx: JavaParser.FormalParameterContext?): Node? {
        return super.visitFormalParameter(ctx)
    }

    override fun visitLastFormalParameter(ctx: JavaParser.LastFormalParameterContext?): Node? {
        return super.visitLastFormalParameter(ctx)
    }

    override fun visitLambdaLVTIList(ctx: JavaParser.LambdaLVTIListContext?): Node? {
        return super.visitLambdaLVTIList(ctx)
    }

    override fun visitLambdaLVTIParameter(ctx: JavaParser.LambdaLVTIParameterContext?): Node? {
        return super.visitLambdaLVTIParameter(ctx)
    }

    override fun visitQualifiedName(ctx: JavaParser.QualifiedNameContext?): Node? {
        return super.visitQualifiedName(ctx)
    }

    override fun visitLiteral(ctx: JavaParser.LiteralContext?): Node? {
        return super.visitLiteral(ctx)
    }

    override fun visitIntegerLiteral(ctx: JavaParser.IntegerLiteralContext?): Node? {
        return super.visitIntegerLiteral(ctx)
    }

    override fun visitFloatLiteral(ctx: JavaParser.FloatLiteralContext?): Node? {
        return super.visitFloatLiteral(ctx)
    }

    override fun visitAltAnnotationQualifiedName(ctx: JavaParser.AltAnnotationQualifiedNameContext?): Node? {
        return super.visitAltAnnotationQualifiedName(ctx)
    }

    override fun visitAnnotation(ctx: JavaParser.AnnotationContext?): Node? {
        return super.visitAnnotation(ctx)
    }

    override fun visitElementValuePairs(ctx: JavaParser.ElementValuePairsContext?): Node? {
        return super.visitElementValuePairs(ctx)
    }

    override fun visitElementValuePair(ctx: JavaParser.ElementValuePairContext?): Node? {
        return super.visitElementValuePair(ctx)
    }

    override fun visitElementValue(ctx: JavaParser.ElementValueContext?): Node? {
        return super.visitElementValue(ctx)
    }

    override fun visitElementValueArrayInitializer(ctx: JavaParser.ElementValueArrayInitializerContext?): Node? {
        return super.visitElementValueArrayInitializer(ctx)
    }

    override fun visitAnnotationTypeDeclaration(ctx: JavaParser.AnnotationTypeDeclarationContext?): Node? {
        return super.visitAnnotationTypeDeclaration(ctx)
    }

    override fun visitAnnotationTypeBody(ctx: JavaParser.AnnotationTypeBodyContext?): Node? {
        return super.visitAnnotationTypeBody(ctx)
    }

    override fun visitAnnotationTypeElementDeclaration(ctx: JavaParser.AnnotationTypeElementDeclarationContext?): Node? {
        return super.visitAnnotationTypeElementDeclaration(ctx)
    }

    override fun visitAnnotationTypeElementRest(ctx: JavaParser.AnnotationTypeElementRestContext?): Node? {
        return super.visitAnnotationTypeElementRest(ctx)
    }

    override fun visitAnnotationMethodOrConstantRest(ctx: JavaParser.AnnotationMethodOrConstantRestContext?): Node? {
        return super.visitAnnotationMethodOrConstantRest(ctx)
    }

    override fun visitAnnotationMethodRest(ctx: JavaParser.AnnotationMethodRestContext?): Node? {
        return super.visitAnnotationMethodRest(ctx)
    }

    override fun visitAnnotationConstantRest(ctx: JavaParser.AnnotationConstantRestContext?): Node? {
        return super.visitAnnotationConstantRest(ctx)
    }

    override fun visitDefaultValue(ctx: JavaParser.DefaultValueContext?): Node? {
        return super.visitDefaultValue(ctx)
    }

    override fun visitModuleDeclaration(ctx: JavaParser.ModuleDeclarationContext?): Node? {
        return super.visitModuleDeclaration(ctx)
    }

    override fun visitModuleBody(ctx: JavaParser.ModuleBodyContext?): Node? {
        return super.visitModuleBody(ctx)
    }

    override fun visitModuleDirective(ctx: JavaParser.ModuleDirectiveContext?): Node? {
        return super.visitModuleDirective(ctx)
    }

    override fun visitRequiresModifier(ctx: JavaParser.RequiresModifierContext?): Node? {
        return super.visitRequiresModifier(ctx)
    }

    override fun visitRecordDeclaration(ctx: JavaParser.RecordDeclarationContext?): Node? {
        return super.visitRecordDeclaration(ctx)
    }

    override fun visitRecordHeader(ctx: JavaParser.RecordHeaderContext?): Node? {
        return super.visitRecordHeader(ctx)
    }

    override fun visitRecordComponentList(ctx: JavaParser.RecordComponentListContext?): Node? {
        return super.visitRecordComponentList(ctx)
    }

    override fun visitRecordComponent(ctx: JavaParser.RecordComponentContext?): Node? {
        return super.visitRecordComponent(ctx)
    }

    override fun visitRecordBody(ctx: JavaParser.RecordBodyContext?): Node? {
        return super.visitRecordBody(ctx)
    }

    override fun visitBlock(ctx: JavaParser.BlockContext?): Node? {
        return super.visitBlock(ctx)
    }

    override fun visitBlockStatement(ctx: JavaParser.BlockStatementContext?): Node? {
        return super.visitBlockStatement(ctx)
    }

    override fun visitLocalVariableDeclaration(ctx: JavaParser.LocalVariableDeclarationContext?): Node? {
        return super.visitLocalVariableDeclaration(ctx)
    }

    /**
     * @return Text node
     */
    override fun visitIdentifier(ctx: JavaParser.IdentifierContext?): Node? {
        return currentDoc.createTextNode(ctx?.text)
    }

    /**
     * @return Text node
     */
    override fun visitTypeIdentifier(ctx: JavaParser.TypeIdentifierContext?): Node? {
        return currentDoc.createTextNode(ctx?.text)
    }

    override fun visitLocalTypeDeclaration(ctx: JavaParser.LocalTypeDeclarationContext?): Node? {
        return super.visitLocalTypeDeclaration(ctx)
    }

    override fun visitStatement(ctx: JavaParser.StatementContext?): Node? {
        return super.visitStatement(ctx)
    }

    override fun visitCatchClause(ctx: JavaParser.CatchClauseContext?): Node? {
        return super.visitCatchClause(ctx)
    }

    override fun visitCatchType(ctx: JavaParser.CatchTypeContext?): Node? {
        return super.visitCatchType(ctx)
    }

    override fun visitFinallyBlock(ctx: JavaParser.FinallyBlockContext?): Node? {
        return super.visitFinallyBlock(ctx)
    }

    override fun visitResourceSpecification(ctx: JavaParser.ResourceSpecificationContext?): Node? {
        return super.visitResourceSpecification(ctx)
    }

    override fun visitResources(ctx: JavaParser.ResourcesContext?): Node? {
        return super.visitResources(ctx)
    }

    override fun visitResource(ctx: JavaParser.ResourceContext?): Node? {
        return super.visitResource(ctx)
    }

    override fun visitSwitchBlockStatementGroup(ctx: JavaParser.SwitchBlockStatementGroupContext?): Node? {
        return super.visitSwitchBlockStatementGroup(ctx)
    }

    override fun visitSwitchLabel(ctx: JavaParser.SwitchLabelContext?): Node? {
        return super.visitSwitchLabel(ctx)
    }

    override fun visitForControl(ctx: JavaParser.ForControlContext?): Node? {
        return super.visitForControl(ctx)
    }

    override fun visitForInit(ctx: JavaParser.ForInitContext?): Node? {
        return super.visitForInit(ctx)
    }

    override fun visitEnhancedForControl(ctx: JavaParser.EnhancedForControlContext?): Node? {
        return super.visitEnhancedForControl(ctx)
    }

    override fun visitParExpression(ctx: JavaParser.ParExpressionContext?): Node? {
        return super.visitParExpression(ctx)
    }

    override fun visitExpressionList(ctx: JavaParser.ExpressionListContext?): Node? {
        return super.visitExpressionList(ctx)
    }

    override fun visitMethodCall(ctx: JavaParser.MethodCallContext?): Node? {
        return super.visitMethodCall(ctx)
    }

    override fun visitExpression(ctx: JavaParser.ExpressionContext?): Node? {
        return super.visitExpression(ctx)
    }

    override fun visitPattern(ctx: JavaParser.PatternContext?): Node? {
        return super.visitPattern(ctx)
    }

    override fun visitLambdaExpression(ctx: JavaParser.LambdaExpressionContext?): Node? {
        return super.visitLambdaExpression(ctx)
    }

    override fun visitLambdaParameters(ctx: JavaParser.LambdaParametersContext?): Node? {
        return super.visitLambdaParameters(ctx)
    }

    override fun visitLambdaBody(ctx: JavaParser.LambdaBodyContext?): Node? {
        return super.visitLambdaBody(ctx)
    }

    override fun visitPrimary(ctx: JavaParser.PrimaryContext?): Node? {
        return super.visitPrimary(ctx)
    }

    override fun visitSwitchExpression(ctx: JavaParser.SwitchExpressionContext?): Node? {
        return super.visitSwitchExpression(ctx)
    }

    override fun visitSwitchLabeledRule(ctx: JavaParser.SwitchLabeledRuleContext?): Node? {
        return super.visitSwitchLabeledRule(ctx)
    }

    override fun visitGuardedPattern(ctx: JavaParser.GuardedPatternContext?): Node? {
        return super.visitGuardedPattern(ctx)
    }

    override fun visitSwitchRuleOutcome(ctx: JavaParser.SwitchRuleOutcomeContext?): Node? {
        return super.visitSwitchRuleOutcome(ctx)
    }

    override fun visitClassType(ctx: JavaParser.ClassTypeContext?): Node? {
        return super.visitClassType(ctx)
    }

    override fun visitCreator(ctx: JavaParser.CreatorContext?): Node? {
        return super.visitCreator(ctx)
    }

    override fun visitCreatedName(ctx: JavaParser.CreatedNameContext?): Node? {
        return super.visitCreatedName(ctx)
    }

    override fun visitInnerCreator(ctx: JavaParser.InnerCreatorContext?): Node? {
        return super.visitInnerCreator(ctx)
    }

    override fun visitArrayCreatorRest(ctx: JavaParser.ArrayCreatorRestContext?): Node? {
        return super.visitArrayCreatorRest(ctx)
    }

    override fun visitClassCreatorRest(ctx: JavaParser.ClassCreatorRestContext?): Node? {
        return super.visitClassCreatorRest(ctx)
    }

    override fun visitExplicitGenericInvocation(ctx: JavaParser.ExplicitGenericInvocationContext?): Node? {
        return super.visitExplicitGenericInvocation(ctx)
    }

    override fun visitTypeArgumentsOrDiamond(ctx: JavaParser.TypeArgumentsOrDiamondContext?): Node? {
        return super.visitTypeArgumentsOrDiamond(ctx)
    }

    override fun visitNonWildcardTypeArgumentsOrDiamond(ctx: JavaParser.NonWildcardTypeArgumentsOrDiamondContext?): Node? {
        return super.visitNonWildcardTypeArgumentsOrDiamond(ctx)
    }

    override fun visitNonWildcardTypeArguments(ctx: JavaParser.NonWildcardTypeArgumentsContext?): Node? {
        return super.visitNonWildcardTypeArguments(ctx)
    }

    override fun visitTypeList(ctx: JavaParser.TypeListContext?): Node? {
        return super.visitTypeList(ctx)
    }

    /**
     * @return Text node
     */
    override fun visitTypeType(ctx: JavaParser.TypeTypeContext?): Node? {
        var typeName: Text? = null

        ctx?.classOrInterfaceType()?.let { typeName = visitClassOrInterfaceType(it) as Text }
        ctx?.primitiveType()?.let { typeName = visitPrimitiveType(it) as Text }

        return typeName
    }

    /**
     * @return Text node
     */
    override fun visitPrimitiveType(ctx: JavaParser.PrimitiveTypeContext?): Node? {
        return currentDoc.createTextNode(ctx?.text)
    }

    override fun visitTypeArguments(ctx: JavaParser.TypeArgumentsContext?): Node? {
        return super.visitTypeArguments(ctx)
    }

    override fun visitSuperSuffix(ctx: JavaParser.SuperSuffixContext?): Node? {
        return super.visitSuperSuffix(ctx)
    }

    override fun visitExplicitGenericInvocationSuffix(ctx: JavaParser.ExplicitGenericInvocationSuffixContext?): Node? {
        return super.visitExplicitGenericInvocationSuffix(ctx)
    }

    override fun visitArguments(ctx: JavaParser.ArgumentsContext?): Node? {
        return super.visitArguments(ctx)
    }
}