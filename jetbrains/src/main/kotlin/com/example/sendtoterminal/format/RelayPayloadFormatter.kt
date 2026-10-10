package com.example.sendtoterminal.format

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.Editor
import com.example.sendtoterminal.settings.AgentProfile
import com.example.sendtoterminal.settings.SendToTerminalSettings

object RelayPayloadFormatter {

    fun formatFileReference(
        project: Project,
        file: VirtualFile,
        agent: AgentProfile,
        settings: SendToTerminalSettings,
    ): RelayPayload {
        val path = relativePath(project, file)
        return AgentReferenceStrategies.forProfile(agent)
            .formatFileReference(path, ReferenceFormatContext)
    }

    fun formatSelection(
        project: Project,
        file: VirtualFile,
        editor: Editor,
        snippet: String,
        agent: AgentProfile,
        settings: SendToTerminalSettings,
    ): RelayPayload {
        val path = relativePath(project, file)
        val (startLine, endLine) = lineRange(editor)
        val language = languageIdForFile(file)
        return AgentReferenceStrategies.forProfile(agent).formatSelection(
            relativePath = path,
            startLine = startLine,
            endLine = endLine,
            language = language,
            snippet = snippet,
            context = ReferenceFormatContext,
        )
    }

    private fun relativePath(project: Project, file: VirtualFile): String {
        val root = ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(file)
            ?: return file.path
        val relative = VfsUtilCore.getRelativePath(file, root, '/')
        return relative ?: file.path
    }

    private fun lineRange(editor: Editor): Pair<Int, Int> {
        val document = editor.document
        val selection = editor.selectionModel
        val startLine = document.getLineNumber(selection.selectionStart) + 1
        val endLine = document.getLineNumber(selection.selectionEnd) + 1
        return startLine to endLine
    }

    private fun languageIdForFile(file: VirtualFile): String {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.name)
        if (fileType is LanguageFileType) {
            return fileType.language.id.lowercase()
        }
        return file.extension?.lowercase() ?: "text"
    }
}
