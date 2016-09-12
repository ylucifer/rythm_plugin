package com.rythmplugin.formatter;

import com.intellij.formatting.*;
import com.intellij.formatting.templateLanguages.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.html.HtmlFormattingModelBuilder;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;

import com.intellij.psi.formatter.DocumentBasedFormattingModel;
import com.intellij.psi.formatter.xml.SyntheticBlock;
import com.intellij.psi.templateLanguages.SimpleTemplateLanguageFormattingModelBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.rythmplugin.RythmLanguage;
import com.rythmplugin.psi.RythmPsiUtil;
import com.rythmplugin.psi.RythmTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Token;

import java.util.List;
import java.util.logging.XMLFormatter;

import static com.rythmplugin.file.RythmFileViewProvider.RYTHM_FRAGMENT;
import static com.rythmplugin.psi.RythmTypes.TEXT;


/**
 * Created by mpl on 04.08.2016.
 */
public class RythmFormattingModelBuilder extends TemplateLanguageFormattingModelBuilder {
    @Override
    public TemplateLanguageBlock createTemplateLanguageBlock(@NotNull ASTNode node,
                                                             @Nullable Wrap wrap,
                                                             @Nullable Alignment alignment,
                                                             @Nullable List<DataLanguageBlockWrapper> foreignChildren,
                                                             @NotNull CodeStyleSettings codeStyleSettings) {


        return new RythmBlock(this, codeStyleSettings, node, foreignChildren);
    }

    @NotNull
    public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
        final PsiFile file = element.getContainingFile();
        Block rootBlock;
        ASTNode node = element.getNode();
        if (node.getElementType() == RYTHM_FRAGMENT) {
            return new SimpleTemplateLanguageFormattingModelBuilder().createModel(element, settings);

        } else {
            rootBlock = getRootBlock(file, file.getViewProvider(), settings);
        }
        return new DocumentBasedFormattingModel(rootBlock, element.getProject(), settings, file.getFileType(), file);
    }

    @Override
    public boolean dontFormatMyModel() {
        return false;
    }

    private static class RythmBlock extends TemplateLanguageBlock {
        RythmBlock(@NotNull TemplateLanguageBlockFactory blockFactory, @NotNull CodeStyleSettings settings, @NotNull ASTNode node, @Nullable List<DataLanguageBlockWrapper> foreignChildren) {
            super(blockFactory, settings, node, foreignChildren);
        }

        @Override
        public Indent getIndent() {
            if (myNode.getText().trim().length() == 0) {
                return Indent.getNoneIndent();
            }
            if (RythmPsiUtil.isNonRootStatementsElement(myNode.getPsi())) {
                if (hasOnlyRythmLanguageParents()) {
                    return Indent.getNormalIndent();
                }
            }
            if (myNode.getTreeParent() != null && RythmPsiUtil.isNonRootStatementsElement(myNode.getTreeParent().getPsi())) {
                if (getParent() instanceof RythmBlock && ((RythmBlock) getParent()).getIndent() == Indent.getNoneIndent()) {
                    return Indent.getNormalIndent();
                }
            }
            if (getRealBlockParent() instanceof DataLanguageBlockWrapper) {
                return Indent.getNormalIndent();
            }
            return Indent.getNoneIndent();
        }

        @Override
        public Alignment getAlignment() {
            return null;
        }

        @Override
        protected IElementType getTemplateTextElementType() {
            return TEXT;
        }

        @Override
        public boolean isRequiredRange(TextRange range) {
            return false;
        }

        @NotNull
        @Override
        public ChildAttributes getChildAttributes(int newChildIndex) {
            if (myNode.getElementType() == RYTHM_FRAGMENT || (getParent() instanceof DataLanguageBlockWrapper && (myNode.getElementType() != RYTHM_FRAGMENT || myNode.getTreeNext() instanceof PsiErrorElement))) {
                return new ChildAttributes(Indent.getNormalIndent(), null);
            } else {
                return new ChildAttributes(Indent.getNoneIndent(), null);
            }
        }

        private boolean hasOnlyRythmLanguageParents() {
            BlockWithParent parent = getParent();
            boolean hasOnlyRythmLanguageParents = true;

            while (parent != null) {
                if (parent instanceof DataLanguageBlockWrapper) {
                    hasOnlyRythmLanguageParents = false;
                    DataLanguageBlockWrapper realParent = (DataLanguageBlockWrapper) parent;
                    break;
                } else {
                    hasOnlyRythmLanguageParents = true;
                }
                parent = parent.getParent();
            }
            return hasOnlyRythmLanguageParents;
        }

        private BlockWithParent getRealBlockParent() {
            BlockWithParent parent = getParent();
            while (parent instanceof DataLanguageBlockWrapper && ((DataLanguageBlockWrapper) parent).getOriginal() instanceof SyntheticBlock) {
                parent = parent.getParent();
            }
            return parent;
        }
    }
}