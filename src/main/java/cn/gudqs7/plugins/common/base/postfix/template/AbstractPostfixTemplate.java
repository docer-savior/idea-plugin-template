package cn.gudqs7.plugins.common.base.postfix.template;

import cn.gudqs7.plugins.common.util.jetbrain.ExceptionUtil;
import cn.gudqs7.plugins.common.util.jetbrain.PsiTypeUtil;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelectorBase;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateWithExpressionSelector;
import com.intellij.codeInsight.template.postfix.util.JavaPostfixTemplatesUtils;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * @author WQ
 */
public abstract class AbstractPostfixTemplate extends PostfixTemplateWithExpressionSelector {

    public AbstractPostfixTemplate(String templateName, String example, Function<PsiElement, Boolean> isApplicable) {
        this(templateName, example, isApplicable, AbstractPostfixTemplate::getExpressions);
    }

    public AbstractPostfixTemplate(String templateName, String example, Function<PsiElement, Boolean> hasExpressionFn, Function<PsiElement, PsiElement> expressionGetFn) {
        super(
                templateName,
                templateName,
                example,
                new PostfixTemplateExpressionSelectorBase(psiElement -> true) {
                    @Override
                    protected List<PsiElement> getNonFilteredExpressions(@NotNull PsiElement context, @NotNull Document document, int offset) {
                        try {
                            return ContainerUtil.createMaybeSingletonList(expressionGetFn.apply(context));
                        } catch (Throwable ex) {
                            ExceptionUtil.handleException(ex);
                            return null;
                        }
                    }

                    @Override
                    protected Condition<PsiElement> getFilters(int offset) {
                        return psiElement -> true;
                    }

                    @Override
                    public boolean hasExpression(@NotNull PsiElement context, @NotNull Document copyDocument, int newOffset) {
                        try {
                            return hasExpressionFn.apply(context);
                        } catch (Throwable ex) {
                            ExceptionUtil.handleException(ex);
                            return false;
                        }
                    }
                },
                null
        );
    }

    /**
     * ???????????? expandForChooseExpression ????????? PsiElement ??????, ????????? isApplicable ??????????????????
     *
     * @param context ?????? psiElement
     * @return ?????? expandForChooseExpression ????????? PsiElement
     */
    private static PsiElement getExpressions(PsiElement context) {
        return JavaPostfixTemplatesUtils.getTopmostExpression(context);
    }

    @Override
    protected void expandForChooseExpression(@NotNull PsiElement expression, @NotNull Editor editor) {
        try {
            expandForChooseExpression0(expression, editor);
        } catch (Throwable e) {
            ExceptionUtil.handleException(e);
        } finally {
            PsiTypeUtil.clearGeneric();
            destroy(expression, editor);
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param expression ???????????????
     * @param editor     ?????????
     */
    protected abstract void expandForChooseExpression0(@NotNull PsiElement expression, @NotNull Editor editor);

    /**
     * ??????????????????????????????????????????
     *
     * @param expression ????????????
     * @param editor     ?????????
     */
    protected void removeExpressionFromEditor(@NotNull PsiElement expression, @NotNull Editor editor) {
        while (!(expression.getParent() instanceof PsiCodeBlock)) {
            expression = expression.getParent();
        }
        Document document = editor.getDocument();
        TextRange textRange = expression.getTextRange();
        int endOffset = textRange.getEndOffset();
        String text = document.getText(new TextRange(endOffset, endOffset + 1));
        boolean expressionEndWithSemicolon = ";".equals(text);
        if (expressionEndWithSemicolon) {
            endOffset = endOffset + 1;
        }
        document.deleteString(textRange.getStartOffset(), endOffset);
    }

    protected void destroy(PsiElement expression, Editor editor) {

    }
}