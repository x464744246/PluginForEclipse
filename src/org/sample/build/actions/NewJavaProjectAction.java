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
		// ��ȡ������
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		// ///////////////////////////////////��������Ŀ///////////////////////////
		final IProject project = root.getProject("xpc");

		// ���ù��̵�λ��
		// Ϊ��Ŀָ�����·��,Ĭ�Ϸ��ڵ�ǰ������
		IPath projectPath = new Path("D:/myplugIn");
		IWorkspace workspace = root.getWorkspace();
		if (!project.exists()) {

			final IProjectDescription description = workspace.newProjectDescription(project.getName());
			description.setLocation(projectPath);

			// ���ù��̱��,��Ϊjava����
			String[] javaNature = description.getNatureIds();
			String[] newJavaNature = new String[javaNature.length + 1];
			System.arraycopy(javaNature, 0, newJavaNature, 0, javaNature.length);
			newJavaNature[javaNature.length] = "org.eclipse.jdt.core.javanature";// ������֤����������Java����
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

				// ���ù��̱��,��Ϊjava����
				String[] javaNature = description.getNatureIds();
				String[] newJavaNature = new String[javaNature.length + 1];
				System.arraycopy(javaNature, 0, newJavaNature, 0, javaNature.length);
				newJavaNature[javaNature.length] = "org.eclipse.jdt.core.javanature";// ������֤����������Java����
				description.setNatureIds(newJavaNature);				
		
				MessageDialog.openWarning(window.getShell(),"�½���Ŀ�ļ�", "ͬ����Ŀ�ļ��Ѿ�����");
				
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		// �������ֻ���½��򵼵�����²ſ���
		/*
		 * //��������Ŀ,WorkspaceModifyOperationλ��org.eclipse.ui.ide��
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
		 * //����������IWizard��getContainer().run()����.
		 */

		// ת����java����
		IJavaProject javaProject = JavaCore.create(project);
		// //////////////////////////////////���JRE��////////////////////////////
		try {
			// ��ȡĬ�ϵ�JRE��
			IClasspathEntry[] jreLibrary = PreferenceConstants.getDefaultJRELibrary();
			// ��ȡԭ����build path
			IClasspathEntry[] oldClasspathEntries = javaProject.getRawClasspath();
			Set<IClasspathEntry> newClasspathEntries = new HashSet<IClasspathEntry>();

			newClasspathEntries.addAll(Arrays.asList(jreLibrary));
			newClasspathEntries.addAll(Arrays.asList(oldClasspathEntries));

			javaProject.setRawClasspath(newClasspathEntries.toArray(new IClasspathEntry[newClasspathEntries.size()]),
					null);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		// //////////////////////////////////�������·��/////////////////////////////
		IFolder binFolder = javaProject.getProject().getFolder("bin");
		if (!binFolder.exists()) {
			try {
				binFolder.create(true, true, null);
				javaProject.setOutputLocation(binFolder.getFullPath(), null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		// /////////////////////////����Java������///////////////////////
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

		// /////////////////////////////����Դ�����ļ���//////////////////////////
		// ///////////Դ�ļ��к��ļ�������,ֻ��ʹ��PackageFragmentRoot�����˷�װ////////
		IFolder srcFolder = javaProject.getProject().getFolder("src");
		if (!srcFolder.exists()) {
			try {
				srcFolder.create(true, true, null);
				// this.createFolder(srcFolder);
				// ����SourceLibrary
				IClasspathEntry srcClasspathEntry = JavaCore.newSourceEntry(srcFolder.getFullPath());

				// �õ��ɵ�build path
				IClasspathEntry[] oldClasspathEntries = javaProject.readRawClasspath();

				// ����µ�
				Set<IClasspathEntry> newClasspathEntries = new HashSet<IClasspathEntry>();
				newClasspathEntries.addAll(Arrays.asList(oldClasspathEntries));
				newClasspathEntries.add(srcClasspathEntry);

				// ԭ������һ���빤������ͬ��Դ�ļ���,������ɾ��
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
		// ///////////////////////////////������//////////////////////////
		// IPackageFragmentRoot packageFragmentRoot =
		// javaProject.getPackageFragmentRoot(javaProject.getResource());
		// �˴��õ���srcĿ¼ֻ��

		try {
			// ����ָ����Դ�ļ������ڵ�IPackageFragmentRoot
			IPackageFragmentRoot packageFragmentRoot = javaProject
					.findPackageFragmentRoot(new Path("/" + project.getName() + "/src"));
			// ����IPackageFragmentRoot����IPackageFragment,IPackageFragment���ǰ���
			IPackageFragment packageFragment = packageFragmentRoot.getPackageFragment("com.myplugin");
			if(!packageFragment.exists()){
			     packageFragment = packageFragmentRoot.createPackageFragment("com.myplugin", true, null);
			}else{
				MessageDialog.openWarning(window.getShell(),"�½����ļ�", "ͬ�����ļ��Ѿ�����");
			}
			// //////////////////////////////////����Java�ļ�////////////////////////
			String javaCode = "package com.myplugin;\n\npublic class HelloPlugin{\n    public static void main(String[] args){\n        System.out.println(\"�л����񹲺͹�\");\n    }\n}";
			if(!packageFragment.getCompilationUnit("HelloPlugin.java").exists()){
			packageFragment.createCompilationUnit("HelloPlugin.java", javaCode, true, new NullProgressMonitor());

			}else{
				MessageDialog.openWarning(window.getShell(),"�½����ļ�", "ͬ�����ļ��Ѿ�����");
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
		// TODO �Զ����ɵķ������

	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}