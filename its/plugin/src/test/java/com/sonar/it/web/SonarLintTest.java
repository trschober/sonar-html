/*
 * SonarSource :: Web :: ITs :: Plugin
 * Copyright (c) 2011-2016 SonarSource SA and Matthijs Galesloot
 * sonarqube@googlegroups.com
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
package com.sonar.it.web;

import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static StandaloneSonarLintEngine sonarlintEngine;

  private static File baseDir;

  @BeforeClass
  public static void prepare() throws Exception {
    StandaloneGlobalConfiguration sonarLintConfig = StandaloneGlobalConfiguration.builder()
      .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../sonar-web-plugin/target"), "sonar-web-plugin-*.jar").getFile().toURI().toURL())
      .setSonarLintUserHome(temp.newFolder().toPath())
      .setLogOutput((formattedMessage, level) -> {
        /* Don't pollute logs */ })
      .build();
    sonarlintEngine = new StandaloneSonarLintEngineImpl(sonarLintConfig);
    baseDir = temp.newFolder();
  }

  @AfterClass
  public static void stop() {
    sonarlintEngine.stop();
  }

  @Test
  public void should_raise_three_issues() throws IOException {
    ClientInputFile inputFile = prepareInputFile("foo.html",
      "<html>\n" +
        "<body>\n" +
        "<a href=\"foo.png\">a</a>\n" +
        "</body>\n" +
        "</html>\n",
      false);

    List<Issue> issues = new ArrayList<>();
    sonarlintEngine.analyze(
      new StandaloneAnalysisConfiguration(baseDir.toPath(), temp.newFolder().toPath(), Collections.singletonList(inputFile), new HashMap<>()),
      issues::add);

    assertThat(issues).extracting("ruleKey", "startLine", "inputFile.path", "severity").containsOnly(
      tuple("Web:DoctypePresenceCheck", 1, inputFile.getPath(), "MAJOR"),
      tuple("Web:LinkToImageCheck", 3, inputFile.getPath(), "MAJOR"),
      tuple("Web:PageWithoutTitleCheck", 1, inputFile.getPath(), "MAJOR"));
  }

  private ClientInputFile prepareInputFile(String relativePath, String content, final boolean isTest) throws IOException {
    File file = new File(baseDir, relativePath);
    FileUtils.write(file, content, StandardCharsets.UTF_8);
    return createInputFile(file.toPath(), isTest);
  }

  private ClientInputFile createInputFile(final Path path, final boolean isTest) {
    return new ClientInputFile() {

      @Override
      public Path getPath() {
        return path;
      }

      @Override
      public boolean isTest() {
        return isTest;
      }

      @Override
      public Charset getCharset() {
        return StandardCharsets.UTF_8;
      }

      @Override
      public <G> G getClientObject() {
        return null;
      }

    };
  }

}