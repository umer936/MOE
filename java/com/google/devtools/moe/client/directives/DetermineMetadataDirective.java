// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Combines the metadata for the given revisions into one consolidated metadata. Useful for when
 * multiple revisions in one repository need to be exported as one revision in the other.
 *
 */
public class DetermineMetadataDirective extends Directive {
  @Option(
    name = "--revisions",
    required = true,
    usage = "Repository expression to get metadata for, e.g. 'internal(revision=3,4)'"
  )
  String repositoryExpression = "";

  private final Ui ui;
  private final Migrator migrator;

  @Inject
  DetermineMetadataDirective(ProjectContextFactory contextFactory, Ui ui, Migrator migrator) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.ui = ui;
    this.migrator = migrator;
  }

  @Override
  protected int performDirectiveBehavior() {
    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(repositoryExpression);
    } catch (ParseError e) {
      ui.error(e, "Couldn't parse " + repositoryExpression);
      return 1;
    }

    List<Revision> revs = Revision.fromRepositoryExpression(repoEx, context());
    RevisionMetadata rm = migrator.processMetadata(context(), revs, null, null);
    ui.info(rm.toString());
    return 0;
  }

  @Override
  public String getDescription() {
    return "Consolidates the metadata for a set of revisions";
  }
}