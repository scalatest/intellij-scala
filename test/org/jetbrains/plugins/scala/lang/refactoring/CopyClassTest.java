package org.jetbrains.plugins.scala.lang.refactoring;

import com.intellij.codeInsight.CodeInsightTestCase;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.search.ProjectScope;
import com.intellij.refactoring.copy.CopyClassesHandler;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.IncorrectOperationException;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author yole
 */
public class CopyClassTest extends CodeInsightTestCase {
  private VirtualFile myRootDir;

  public void testReplaceAllOccurrences() throws Exception {
    doTest("Foo", "Bar");
  }

  // TODO
//  public void testLibraryClass() throws Exception {  // IDEADEV-28791
//    doTest("java.util.ArrayList", "Bar");
//  }

  private void doTest(final String oldName, final String copyName) throws Exception {
    String root = JavaTestUtil.getJavaTestDataPath() + "/refactoring/copyClass/" + getTestName(true);

    PsiTestUtil.removeAllRoots(myModule, JavaSdkImpl.getMockJdk17("java 1.5"));
    myRootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, root, myFilesToDelete);

    PsiElement element = performAction(oldName, copyName);

    myProject.getComponent(PostprocessReformattingAspect.class).doPostponedFormatting();
    FileDocumentManager.getInstance().saveAllDocuments();

    VirtualFile fileAfter = myRootDir.findChild(copyName + ".java");
    VirtualFile fileExpected = myRootDir.findChild(copyName + ".expected.java");

    PlatformTestUtil.assertFilesEqual(fileExpected, fileAfter);
  }

  private PsiElement performAction(final String oldName, final String copyName) throws IncorrectOperationException {
    PsiClass oldClass = JavaPsiFacade.getInstance(myProject).findClass(oldName, ProjectScope.getAllScope(myProject));
    return CopyClassesHandler.doCopyClasses(Collections.singletonMap(oldClass.getNavigationElement().getContainingFile(), new PsiClass[]{oldClass}), copyName, myPsiManager.findDirectory(myRootDir),
        myProject);
  }

  public void testPackageLocalClasses() throws Exception {
    doMultifileTest();
  }

  public void testPackageLocalMethods() throws Exception {
    doMultifileTest();
  }

  //copy all classes from p1 -> p2
  private void doMultifileTest() throws Exception {
    String root = JavaTestUtil.getJavaTestDataPath() + "/refactoring/copyClass/multifile/" + getTestName(true);
    String rootBefore = root + "/before";
    PsiTestUtil.removeAllRoots(myModule, JavaSdkImpl.getMockJdk17());
    VirtualFile rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, rootBefore, myFilesToDelete);

    final HashMap<PsiFile, PsiClass[]> map = new HashMap<PsiFile, PsiClass[]>();
    final VirtualFile sourceDir = rootDir.findChild("p1");
    for (VirtualFile file : sourceDir.getChildren()) {
      final PsiFile psiFile = myPsiManager.findFile(file);
      if (psiFile instanceof PsiJavaFile) {
        map.put(psiFile, ((PsiJavaFile)psiFile).getClasses());
      }
    }

    final VirtualFile targetVDir = rootDir.findChild("p2");
    CopyClassesHandler.doCopyClasses(map, null, myPsiManager.findDirectory(targetVDir), myProject);

    String rootAfter = root + "/after";
    VirtualFile rootDir2 = LocalFileSystem.getInstance().findFileByPath(rootAfter.replace(File.separatorChar, '/'));
    myProject.getComponent(PostprocessReformattingAspect.class).doPostponedFormatting();
    PlatformTestUtil.assertDirectoriesEqual(rootDir2, rootDir, PlatformTestUtil.CVS_FILE_FILTER);
  }

  public void testPackageHierarchy() throws Exception {
    doPackageCopy();
  }

  public void testPackageOneLevelHierarchy() throws Exception {
    doPackageCopy();
  }

  private void doPackageCopy() throws Exception {
    String root = JavaTestUtil.getJavaTestDataPath() + "/refactoring/copyClass/multifile/" + getTestName(true);
    String rootBefore = root + "/before";
    PsiTestUtil.removeAllRoots(myModule, JavaSdkImpl.getMockJdk17());
    VirtualFile rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, rootBefore, myFilesToDelete);

    final VirtualFile targetVDir = rootDir.findChild("p2");
    final PsiDirectory sourceP1Dir = myPsiManager.findDirectory(rootDir.findChild("p1"));
    final PsiDirectory targetP2Dir = myPsiManager.findDirectory(targetVDir);
    new CopyClassesHandler().doCopy(new PsiElement[]{sourceP1Dir}, targetP2Dir);

    String rootAfter = root + "/after";
    VirtualFile rootDir2 = LocalFileSystem.getInstance().findFileByPath(rootAfter.replace(File.separatorChar, '/'));
    myProject.getComponent(PostprocessReformattingAspect.class).doPostponedFormatting();
    PlatformTestUtil.assertDirectoriesEqual(rootDir2, rootDir, PlatformTestUtil.CVS_FILE_FILTER);
  }
}
