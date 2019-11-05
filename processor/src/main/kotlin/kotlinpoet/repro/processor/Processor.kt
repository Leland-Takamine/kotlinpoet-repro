package kotlinpoet.repro.processor

import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

class Processor : AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf("*")
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val kotlinSourceDirectory = File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION]!!)
        roundEnv.rootElements
            .filterIsInstance<TypeElement>()
            .filter { Modifier.ABSTRACT in it.modifiers }
            .map(this::generateSubclass)
            .forEach { fileSpec ->
                fileSpec.writeTo(kotlinSourceDirectory)
            }
        return true
    }

    private fun generateSubclass(element: TypeElement): FileSpec {
        val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()
        val className = "${element.simpleName}Impl"
        return FileSpec.builder(packageName, className).apply {
            addType(TypeSpec.Companion.classBuilder(ClassName(packageName, className)).apply {
                superclass(element.asClassName())
                addFunctions(ElementFilter.methodsIn(element.enclosedElements)
                    .filter { method -> Modifier.ABSTRACT in method.modifiers }
                    .map { method -> generateOverridingMethod(method) })
            }.build())
        }.build()
    }

    private fun generateOverridingMethod(method: ExecutableElement): FunSpec {
        return FunSpec.overriding(method)
            .addStatement("throw UnsupportedOperationException()")
            .build()
    }

    private companion object {
        private val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
    }
}