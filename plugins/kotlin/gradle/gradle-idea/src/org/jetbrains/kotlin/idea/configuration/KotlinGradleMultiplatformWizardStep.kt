/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.ProjectWizardUtil
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.PanelWithAnchor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.event.DocumentEvent

class KotlinGradleMultiplatformWizardStep(
    private val builder: KotlinGradleMultiplatformModuleBuilder,
    private val wizardContext: WizardContext
) : ModuleWizardStep() {

    private val hierarchyKindComponent: LabeledComponent<JComboBox<String>> =
        LabeledComponent.create(
            JComboBox(
                arrayOf(
                    "Root empty module with common and platform modules as children",
                    "Root common module with children platform modules"
                )
            ), "Hierarchy kind", BorderLayout.WEST
        )
    private val rootModuleNameComponent: LabeledComponent<JTextField> =
        LabeledComponent.create(JTextField(), "Root module name:", BorderLayout.WEST)
    private val commonModuleNameComponent: LabeledComponent<JTextField> =
        LabeledComponent.create(JTextField(), "Common module name:", BorderLayout.WEST)
    private val jvmCheckBox: JCheckBox =
        JCheckBox("Create JVM module", true)
    private val jdkModel = ProjectSdksModel().also {
        it.reset(ProjectManager.getInstance().defaultProject)
    }
    private val jdkComboBox: JdkComboBox =
        JdkComboBox(jdkModel) { it is JavaSdk }
    private val jvmModuleNameComponent: LabeledComponent<JTextField> =
        LabeledComponent.create(JTextField(), "JVM module name:", BorderLayout.WEST)
    private val jsCheckBox: JCheckBox =
        JCheckBox("Create JS module", true)
    private val jsModuleNameComponent: LabeledComponent<JTextField> =
        LabeledComponent.create(JTextField(), "JS module name:", BorderLayout.WEST)

    private val panel: JPanel
    private var syncEditing: Boolean = true
    private var inSyncUpdate: Boolean = false

    init {
        panel = object : JPanel(GridBagLayout()), PanelWithAnchor {
            private var anchor: JComponent? = hierarchyKindComponent.anchor

            override fun getAnchor(): JComponent? = anchor

            override fun setAnchor(anchor: JComponent?) {
                this.anchor = anchor
                hierarchyKindComponent.anchor = anchor
                rootModuleNameComponent.anchor = anchor
                commonModuleNameComponent.anchor = anchor
                jvmModuleNameComponent.anchor = anchor
                jsModuleNameComponent.anchor = anchor
            }
        }
        val baseDir = wizardContext.projectFileDirectory
        val projectName = wizardContext.projectName
        val initialProjectName = projectName ?: ProjectWizardUtil.findNonExistingFileName(baseDir, "untitled", "")
        rootModuleNameComponent.component.text = initialProjectName
        rootModuleNameComponent.component.select(0, initialProjectName.length)

        updateDerivedModuleNames()

        rootModuleNameComponent.component.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent?) {
                if (syncEditing) {
                    updateDerivedModuleNames()
                }
            }
        })

        val stopSyncEditingListener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent?) {
                if (!inSyncUpdate) {
                    syncEditing = false
                }
            }
        }
        commonModuleNameComponent.component.document.addDocumentListener(stopSyncEditingListener)
        jvmModuleNameComponent.component.document.addDocumentListener(stopSyncEditingListener)
        jsModuleNameComponent.component.document.addDocumentListener(stopSyncEditingListener)

        jdkComboBox.selectedJdk = jdkModel.projectSdk

        hierarchyKindComponent.component.addActionListener {
            commonModuleNameComponent.isEnabled = !commonModuleIsRoot
        }
        jvmCheckBox.addItemListener {
            jvmModuleNameComponent.isEnabled = jvmCheckBox.isSelected
            jdkComboBox.isEnabled = jvmCheckBox.isSelected
        }
        jsCheckBox.addItemListener {
            jsModuleNameComponent.isEnabled = jsCheckBox.isSelected
        }

        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        panel.add(
            hierarchyKindComponent,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            rootModuleNameComponent,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.insets(10, 0, 0, 0), 0, 0
            )
        )
        panel.add(
            commonModuleNameComponent,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            jvmCheckBox,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            jdkComboBox,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            jvmModuleNameComponent,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            jsCheckBox,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            jsModuleNameComponent,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            JLabel(""),
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        UIUtil.mergeComponentsWithAnchor(panel)
    }

    private fun updateDerivedModuleNames() {
        inSyncUpdate = true
        try {
            commonModuleNameComponent.component.text = "$rootModuleName-common"
            jvmModuleNameComponent.component.text = "$rootModuleName-jvm"
            jsModuleNameComponent.component.text = "$rootModuleName-js"

        } finally {
            inSyncUpdate = false
        }
    }

    override fun updateDataModel() {
        wizardContext.projectBuilder = builder
        wizardContext.projectName = rootModuleName

        builder.projectId = ProjectId("", rootModuleName, "")
        builder.commonModuleName = commonModuleName
        builder.jvmModuleName = jvmModuleName
        builder.jdk = jdk
        builder.jsModuleName = jsModuleName
    }

    override fun getComponent() = panel

    private val commonModuleIsRoot: Boolean
        get() = hierarchyKindComponent.component.selectedIndex != 0
    private val rootModuleName: String
        get() = rootModuleNameComponent.component.text
    private val commonModuleName: String
        get() = if (commonModuleIsRoot) "" else commonModuleNameComponent.component.text
    private val jvmModuleName: String
        get() = if (jvmCheckBox.isSelected) jvmModuleNameComponent.component.text else ""
    private val jdk: Sdk?
        get() = if (jvmCheckBox.isSelected) jdkComboBox.selectedJdk else null
    private val jsModuleName: String
        get() = if (jsCheckBox.isSelected) jsModuleNameComponent.component.text else ""

    override fun validate(): Boolean {
        if (rootModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the root module name")
        }
        if (!commonModuleIsRoot && commonModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the common module name")
        }
        if (jvmCheckBox.isSelected && jvmModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the JVM module name")
        }
        if (jsCheckBox.isSelected && jsModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the JS module name")
        }
        if (commonModuleName.isNotEmpty()
            && (commonModuleName == rootModuleName || commonModuleName == jvmModuleName || commonModuleName == jsModuleName)
        ) {
            throw ConfigurationException("The common module name should be distinct")
        }
        if (jvmModuleName.isNotEmpty() && (jvmModuleName == rootModuleName || jvmModuleName == jsModuleName)) {
            throw ConfigurationException("The JVM module name should be distinct")
        }
        if (jsModuleName.isNotEmpty() && jsModuleName == rootModuleName) {
            throw ConfigurationException("The IS module name should be distinct")
        }
        return true
    }
}
