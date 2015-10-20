package org.sample.build.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class NewJavaProjectAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	public void run(IAction action) {
		// 获取工作区
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		// ///////////////////////////////////创建新项目///////////////////////////
		final IProject project = root.getProject("xpc");

		// 设置工程的位置
		// 为项目指定存放路径,默认放在当前工作区
		IPath projectPath = new Path("D:/myplugIn");
		IWorkspace workspace = root.getWorkspace();
		if (!project.exists()) {

			final IProjectDescription description = workspace.newProjectDescription(project.getName());
			description.setLocation(projectPath);

			// 设置工程标记,即为java工程
			String[] javaNature = description.getNatureIds();
			String[] newJavaNature = new String[javaNature.length + 1];
			System.arraycopy(javaNature, 0, newJavaNature, 0, javaNature.length);
			newJavaNature[javaNature.length] = "org.eclipse.jdt.core.javanature";// 这个标记证明本工程是Java工程
			description.setNatureIds(newJavaNature);

			// /////////////////////////////
			try {
				NullProgressMonitor monitor = new NullProgressMonitor();
				project.create(description, monitor);
				project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		} else {
			try {
				final IProjectDescription description = project.getDescription();
				description.setLocation(projectPath);

				// 设置工程标记,即为java工程
				String[] javaNature = description.getNatureIds();
				String[] newJavaNature = new String[javaNature.length + 1];
				System.arraycopy(javaNature, 0, newJavaNature, 0, javaNature.length);
				newJavaNature[javaNature.length] = "org.eclipse.jdt.core.javanature";// 这个标记证明本工程是Java工程
				description.setNatureIds(newJavaNature);				
		
				MessageDialog.openWarning(window.getShell(),"新建项目文件", "同名项目文件已经存在");
				
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		// 下面代码只在新建向导的情况下才可用
		/*
		 * //创建新项目,WorkspaceModifyOperation位于org.eclipse.ui.ide中
		 * WorkspaceModifyOperation workspaceModifyOperation = new
		 * WorkspaceModifyOperation() {
		 * 
		 * @Override protected void execute(IProgressMonitor monitor) throws
		 * CoreException, InvocationTargetException, InterruptedException { try
		 * { monitor.beginTask("", ); project.create(description, monitor);
		 * 
		 * if(monitor.isCanceled()){ throw new OperationCanceledException(); }
		 * 
		 * project.open(IResource.BACKGROUND_REFRESH, new
		 * SubProgressMonitor(monitor, )); } catch (Exception e) {
		 * e.printStackTrace(); } finally{ monitor.done(); } } };
		 * //接下来调用IWizard的getContainer().run()方法.
		 */

		// 转化成java工程
		IJavaProject javaProject = JavaCore.create(project);
		// //////////////////////////////////添加JRE库////////////////////////////
		try {
			// 获取默认的JRE库
			IClasspathEntry[] jreLibrary = PreferenceConstants.getDefaultJRELibrary();
			// 获取原来的build path
			IClasspathEntry[] oldClasspathEntries = javaProject.getRawClasspath();
			Set<IClasspathEntry> newClasspathEntries = new HashSet<IClasspathEntry>();

			newClasspathEntries.addAll(Arrays.asList(jreLibrary));
			newClasspathEntries.addAll(Arrays.asList(oldClasspathEntries));

			javaProject.setRawClasspath(newClasspathEntries.toArray(new IClasspathEntry[newClasspathEntries.size()]),
					null);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		// //////////////////////////////////创建输出路径/////////////////////////////
		IFolder binFolder = javaProject.getProject().getFolder("bin");
		if (!binFolder.exists()) {
			try {
				binFolder.create(true, true, null);
				javaProject.setOutputLocation(binFolder.getFullPath(), null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		// /////////////////////////设置Java生成器///////////////////////
		try {
			IProjectDescription description2 = javaProject.getProject().getDescription();
			ICommand command = description2.newCommand();
			command.setBuilderName("org.eclipse.jdt.core.javabuilder");
			description2.setBuildSpec(new ICommand[] { command });
			description2.setNatureIds(new String[] { "org.eclipse.jdt.core.javanature" });
			javaProject.getProject().setDescription(description2, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		// /////////////////////////////创建源代码文件夹//////////////////////////
		// ///////////源文件夹和文件夹相似,只是使用PackageFragmentRoot进行了封装////////
		IFolder srcFolder = javaProject.getProject().getFolder("src");
		if (!srcFolder.exists()) {
			try {
				srcFolder.create(true, true, null);
				// this.createFolder(srcFolder);
				// 创建SourceLibrary
				IClasspathEntry srcClasspathEntry = JavaCore.newSourceEntry(srcFolder.getFullPath());

				// 得到旧的build path
				IClasspathEntry[] oldClasspathEntries = javaProject.readRawClasspath();

				// 添加新的
				Set<IClasspathEntry> newClasspathEntries = new HashSet<IClasspathEntry>();
				newClasspathEntries.addAll(Arrays.asList(oldClasspathEntries));
				newClasspathEntries.add(srcClasspathEntry);

				// 原来存在一个与工程名相同的源文件夹,必须先删除
				IClasspathEntry temp = JavaCore.newSourceEntry(new Path("/xpc"));
				if (newClasspathEntries.contains(temp)) {
					newClasspathEntries.remove(temp);
				}

				// System.out.println(list.size());

				javaProject.setRawClasspath(
						newClasspathEntries.toArray(new IClasspathEntry[newClasspathEntries.size()]), null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		// ///////////////////////////////创建包//////////////////////////
		// IPackageFragmentRoot packageFragmentRoot =
		// javaProject.getPackageFragmentRoot(javaProject.getResource());
		// 此处得到的src目录只读

		try {
			// 先找指定的源文件夹所在的IPackageFragmentRoot
			IPackageFragmentRoot packageFragmentRoot = javaProject
					.findPackageFragmentRoot(new Path("/" + project.getName() + "/src"));
			// 根据IPackageFragmentRoot创建IPackageFragment,IPackageFragment就是包了
			IPackageFragment packageFragment = packageFragmentRoot.getPackageFragment("com.myplugin");
			if(!packageFragment.exists()){
			     packageFragment = packageFragmentRoot.createPackageFragment("com.myplugin", true, null);
			}else{
				MessageDialog.openWarning(window.getShell(),"新建包文件", "同名包文件已经存在");
			}
			// //////////////////////////////////创建Java文件////////////////////////
			String javaCode = "package com.myplugin;\n\npublic class HelloPlugin{\n    public static void main(String[] args){\n        System.out.println(\"中华人民共和国\");\n    }\n}";
			if(!packageFragment.getCompilationUnit("HelloPlugin.java").exists()){
			packageFragment.createCompilationUnit("HelloPlugin.java", javaCode, true, new NullProgressMonitor());

			}else{
				MessageDialog.openWarning(window.getShell(),"新建类文件", "同名类文件已经存在");
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
		// TODO 自动生成的方法存根

	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}