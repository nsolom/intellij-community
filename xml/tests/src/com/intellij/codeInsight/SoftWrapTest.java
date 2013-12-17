/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.SoftWrapModelImpl;
import com.intellij.openapi.editor.impl.softwrap.mapping.SoftWrapApplianceManager;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

/**
 * @author Dmitry Avdeev
 *         Date: 25.11.13
 */
public class SoftWrapTest extends LightPlatformCodeInsightFixtureTestCase {

  public void testSoftWrapInInjection() throws Exception {

    myFixture.configureByFile("softWrap.html");
    Editor editor = myFixture.getEditor();
    editor.getSettings().setUseSoftWraps(true);
    ((SoftWrapModelImpl)editor.getSoftWrapModel()).getApplianceManager().setWidthProvider(new SoftWrapApplianceManager.VisibleAreaWidthProvider() {
      @Override
      public int getVisibleAreaWidth() {
        return 600;
      }
    });

    myFixture.type('j');
    myFixture.checkResultByFile("softWrap_after.html");
  }

  @Override
  protected String getBasePath() {
    return "/xml/tests/testData/softWrap";
  }

  @Override
  protected boolean isCommunity() {
    return true;
  }
}
