// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.IdentityEditor;
import com.google.devtools.moe.client.editors.ScrubbingEditor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.editors.TranslatorStep;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.svn.SvnRepository;
import com.google.devtools.moe.client.testing.DummyRepository;

import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ProjectContext {

  public final ProjectConfig config;
  public final ImmutableMap<String, Repository> repositories;
  public final ImmutableMap<String, Editor> editors;
  public final ImmutableMap<TranslatorPath, Translator> translators;

  public ProjectContext(
      ProjectConfig config,
      ImmutableMap<String, Repository> repositories,
      ImmutableMap<String, Editor> editors,
      ImmutableMap<TranslatorPath, Translator> translators) {
    this.config = config;
    this.repositories = repositories;
    this.editors = editors;
    this.translators = translators;
  }

  public static ProjectContext makeProjectContextFromConfigText(String configText)
      throws InvalidProject {
    ProjectConfig config =
        ProjectConfig.makeProjectConfigFromConfigText(configText);

    ImmutableMap.Builder<String, Repository> b = ImmutableMap.builder();
    for (Map.Entry<String, RepositoryConfig> e :
             config.getRepositoryConfigs().entrySet()) {
      b.put(e.getKey(), makeRepositoryFromConfig(e.getKey(), e.getValue()));
    }
    ImmutableMap<String, Repository> repositories = b.build();

    ImmutableMap.Builder<String, Editor> editorsBuilder = ImmutableMap.builder();
    for (Map.Entry<String, EditorConfig> entry :
             config.getEditorConfigs().entrySet()) {
      editorsBuilder.put(entry.getKey(), makeEditorFromConfig(entry.getKey(), entry.getValue()));
    }
    ImmutableMap<String, Editor> editors = editorsBuilder.build();

    ImmutableMap.Builder<TranslatorPath, Translator> translatorsBuilder = ImmutableMap.builder();
    for (TranslatorConfig tc : config.getTranslators()) {
      Translator t = makeTranslatorFromConfig(tc);
      translatorsBuilder.put(t.getTranslatorPath(), t);
    }
    ImmutableMap<TranslatorPath, Translator> translators = translatorsBuilder.build();

    return new ProjectContext(config, repositories, editors, translators);
  }

  public static Repository makeRepositoryFromConfig(
      String repositoryName, RepositoryConfig config) throws InvalidProject {
    switch (config.getType()) {
      case svn:
        return SvnRepository.makeSvnRepositoryFromConfig(
            repositoryName, config);
      case dummy:
        return DummyRepository.makeDummyRepository(repositoryName, config);
      default:
        throw new InvalidProject(
            "Invalid repository type: \"" + config.getType() + "\"");
    }
  }

  public static Editor makeEditorFromConfig(
      String editorName, EditorConfig config) throws InvalidProject {
    switch (config.getType()) {
      case identity:
        return IdentityEditor.makeIdentityEditor(editorName, config);
      case scrubber:
        return ScrubbingEditor.makeScrubbingEditor(editorName, config);
      default:
        throw new InvalidProject(
            String.format("Invalid editor type: \"%s\"", config.getType()));
    }
  }

  public static Translator makeTranslatorFromConfig(TranslatorConfig config)
      throws InvalidProject {
    ImmutableList.Builder<TranslatorStep> steps = ImmutableList.builder();
    for (StepConfig sc: config.getSteps()) {
      steps.add(new TranslatorStep(sc.getName(),
                                   makeEditorFromConfig(sc.getName(), sc.getEditorConfig())));
    }
    return new Translator(
        config.getFromProjectSpace(), config.getToProjectSpace(), steps.build());
  }

  public static class Builder {
    public ProjectConfig config;
    public ImmutableMap<String, Repository> repositories;
    public ImmutableMap<String, Editor> editors;
    public ImmutableMap<TranslatorPath, Translator> translators;

    public Builder() {
      config = null;
      repositories = ImmutableMap.of();
      editors = ImmutableMap.of();
      translators = ImmutableMap.of();
    }

    public Builder withRepositories(ImmutableMap<String, Repository> repositories) {
      this.repositories = repositories;
      return this;
    }

    public Builder withEditors(ImmutableMap<String, Editor> editors) {
      this.editors = editors;
      return this;
    }

    public Builder withTranslators(ImmutableMap<TranslatorPath, Translator> translators) {
      this.translators = translators;
      return this;
    }

    public ProjectContext build() {
      return new ProjectContext(config, repositories, editors, translators);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}