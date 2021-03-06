package org.jetbrains.plugins.scala.lang.psi.impl

import com.intellij.psi.impl.file.PsiPackageImpl
import org.jetbrains.plugins.scala.lang.psi.api.ScPackage
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.ScalaFileType
import com.intellij.psi._
import impl.PsiManagerEx
import java.lang.String
import scope.{NameHint, ElementClassHint, PsiScopeProcessor}
import toplevel.synthetic.SyntheticClasses
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.caches.{ScalaShortNamesCacheManager, CachesUtil}
import org.jetbrains.plugins.scala.extensions.toPsiNamedElementExt
import org.jetbrains.plugins.scala.lang.resolve.ResolveUtils
import org.jetbrains.plugins.scala.lang.resolve.processor.{ResolveProcessor, ImplicitProcessor}

/**
 * User: Alexander Podkhalyuzin
 * Date: 22.04.2010
 */
class ScPackageImpl(val pack: PsiPackage) extends PsiPackageImpl(pack.getManager.asInstanceOf[PsiManagerEx],
        pack.getQualifiedName) with ScPackage {
  def superProcessDeclarations(processor: PsiScopeProcessor, state: ResolveState,
                                    lastParent: PsiElement, place: PsiElement): Boolean = {
    super.processDeclarations(processor, state, lastParent, place)
  }

  override def processDeclarations(processor: PsiScopeProcessor, state: ResolveState,
                                   lastParent: PsiElement, place: PsiElement): Boolean = {
    if (place.getLanguage == ScalaFileType.SCALA_LANGUAGE && pack.getQualifiedName == "scala") {
      if (!processor.isInstanceOf[ImplicitProcessor]) {
        val scope = processor match {
          case r: ResolveProcessor => r.getResolveScope
          case _ => place.getResolveScope
        }
        val namesSet = ScalaShortNamesCacheManager.getInstance(getProject).getClassNames(pack, scope)

        //Process synthetic classes for scala._ package
        /**
         * Does the "scala" package already contain a class named `className`?
         *
         * [[http://youtrack.jetbrains.net/issue/SCL-2913]]
         */
        def alreadyContains(className: String) = namesSet.contains(className)

        for (synth <- SyntheticClasses.get(getProject).getAll) {
          if (!alreadyContains(synth.name)) processor.execute(synth, ResolveState.initial)
        }
        for (synthObj <- SyntheticClasses.get(getProject).syntheticObjects) {

          // Assume that is the scala package contained a class with the same names as the synthetic object,
          // then it must also contain the object.
          if (!alreadyContains(synthObj.name)) processor.execute(synthObj, ResolveState.initial)
        }
      }
    } else {
      if (!ResolveUtils.packageProcessDeclarations(pack, processor, state, lastParent, place)) return false
    }

    //for Scala
    if (place.getLanguage == ScalaFileType.SCALA_LANGUAGE) {
      val scope = processor match {
        case r: ResolveProcessor => r.getResolveScope
        case _ => place.getResolveScope
      }
      if (getQualifiedName == "scala") {
        ImplicitlyImported.implicitlyImportedObject(place.getManager, scope, "scala") match {
          case Some(obj) =>
            if (!obj.processDeclarations(processor, state, lastParent, place)) return false
          case _ =>
        }
      } else {
        findPackageObject(scope) match {
          case Some(obj) =>
            if (!obj.processDeclarations(processor, state, lastParent, place)) return false
          case None =>
        }
      }
    }
    true
  }

  private def findSubPackageByName(name: String): PsiPackage = {
    val qName: String = getQualifiedName
    val subpackageQName: String = if (qName.length > 0) qName + "." + name else name
    val aPackage: PsiPackage = JavaPsiFacade.getInstance(getProject).findPackage(subpackageQName)
    if (aPackage == null) return null
    aPackage
  }

  private def findClassesByName(name: String, scope: GlobalSearchScope): Array[PsiClass] = {
    val qName: String = getQualifiedName
    val classQName: String = if (qName.length > 0) qName + "." + name else name
    ScalaPsiManager.instance(getProject).getCachedClasses(scope, classQName)
  }

  private def processClassesByName(processor: PsiScopeProcessor, state: ResolveState, place: PsiElement,
                                   scope: GlobalSearchScope, className: String): Boolean = {
    val classes: Array[PsiClass] = findClassesByName(className, scope)
    !processClasses(processor, state, classes)
  }

  private def processClasses(processor: PsiScopeProcessor, state: ResolveState, classes: Array[PsiClass]): Boolean = {
    val iter = classes.iterator
    while (iter.hasNext) {
      val aClass = iter.next()
      if (!processor.execute(aClass, state)) return false
    }
    true
  }

  def findPackageObject(scope: GlobalSearchScope): Option[ScTypeDefinition] = {
    val manager = ScalaShortNamesCacheManager.getInstance(getProject)

    var tuple = pack.getUserData(CachesUtil.PACKAGE_OBJECT_KEY)
    val count = getManager.getModificationTracker.getOutOfCodeBlockModificationCount
    if (tuple == null || tuple._2.longValue != count) {
      val clazz = manager.getPackageObjectByName(getQualifiedName, scope)
      tuple = (clazz, java.lang.Long.valueOf(count)) // TODO is it safe to cache this ignoring `scope`?
      pack.putUserData(CachesUtil.PACKAGE_OBJECT_KEY, tuple)
    }
    Option(tuple._1)
  }

  override def getParentPackage: PsiPackageImpl = {
    val myQualifiedName = getQualifiedName
    if (myQualifiedName.length == 0) return null
    val lastDot: Int = myQualifiedName.lastIndexOf('.')
    if (lastDot < 0) {
      ScPackageImpl.findPackage(getProject, "")
    } else {
      ScPackageImpl.findPackage(getProject, myQualifiedName.substring(0, lastDot))
    }
  }

  override def getSubPackages: Array[PsiPackage] = {
    super.getSubPackages.map(ScPackageImpl(_))
  }

  override def getSubPackages(scope: GlobalSearchScope): Array[PsiPackage] = {
    super.getSubPackages(scope).map(ScPackageImpl(_))
  }
}

object ScPackageImpl {
  def apply(pack: PsiPackage): ScPackageImpl = {
    if (pack == null) null
    else if (pack.isInstanceOf[ScPackageImpl]) pack.asInstanceOf[ScPackageImpl]
    else new ScPackageImpl(pack)
  }

  def findPackage(project: Project, pName: String) = {
    ScPackageImpl(JavaPsiFacade.getInstance(project).findPackage(pName))
  }
}