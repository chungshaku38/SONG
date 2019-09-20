/*
 * Copyright (c) 2018. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package bio.overture.song.client.cli;

import static bio.overture.song.core.exceptions.ServerErrors.UNAUTHORIZED_TOKEN;
import static bio.overture.song.core.exceptions.ServerErrors.UNKNOWN_ERROR;
import static bio.overture.song.core.exceptions.SongError.createSongError;

import bio.overture.song.client.command.ConfigCommand;
import bio.overture.song.client.command.ExportCommand;
import bio.overture.song.client.command.FileUpdateCommand;
import bio.overture.song.client.command.GetAnalysisTypeCommand;
import bio.overture.song.client.command.ListAnalysisTypesCommand;
import bio.overture.song.client.command.ManifestCommand;
import bio.overture.song.client.command.PingCommand;
import bio.overture.song.client.command.PublishCommand;
import bio.overture.song.client.command.RegisterAnalysisTypeCommand;
import bio.overture.song.client.command.SearchCommand;
import bio.overture.song.client.command.SubmitCommand;
import bio.overture.song.client.command.SuppressCommand;
import bio.overture.song.client.command.UnpublishCommand;
import bio.overture.song.client.config.Config;
import bio.overture.song.client.register.ErrorStatusHeader;
import bio.overture.song.client.register.Registry;
import bio.overture.song.core.exceptions.ServerException;
import bio.overture.song.core.exceptions.SongError;
import java.io.IOException;
import java.net.HttpRetryException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;

@Slf4j
@Configuration
public class ClientMain implements CommandLineRunner {

  private CommandParser dispatcher;
  private ErrorStatusHeader errorStatusHeader;
  private Registry registry;
  private Config config;

  @Autowired
  public ClientMain(Config config, Registry registry) {
    val programName = config.getProgramName();
    val options = new Options();

    val builder = new CommandParserBuilder(programName, options);
    builder.register("config", new ConfigCommand(config));
    builder.register("upload", new SubmitCommand(registry));
    builder.register("ping", new PingCommand(registry));
    builder.register("get-analysis-type", new GetAnalysisTypeCommand(registry));
    builder.register("list-analysis-types", new ListAnalysisTypesCommand(registry));
    builder.register("register-analysis-type", new RegisterAnalysisTypeCommand(registry));
    builder.register("search", new SearchCommand(registry, config));
    builder.register("manifest", new ManifestCommand(registry, config));
    builder.register("publish", new PublishCommand(registry, config));
    builder.register("unpublish", new UnpublishCommand(registry, config));
    builder.register("suppress", new SuppressCommand(registry, config));
    builder.register("export", new ExportCommand(registry));
    builder.register("update-file", new FileUpdateCommand(config, registry));
    this.dispatcher = builder.build();
    this.errorStatusHeader = new ErrorStatusHeader(config);
    this.registry = registry;
    this.config = config;
  }

  @Override
  public void run(String... args) {
    val command = dispatcher.parse(args);
    try {
      command.run();
    } catch (RestClientException e) {
      val isAlive = registry.isAlive();
      SongError songError;
      if (isAlive) {
        val cause = e.getCause();
        if (cause instanceof HttpRetryException) {
          val httpRetryException = (HttpRetryException) cause;
          if (httpRetryException.responseCode() == UNAUTHORIZED_TOKEN.getHttpStatus().value()) {
            songError = createSongError(UNAUTHORIZED_TOKEN, "Invalid token");
          } else {
            songError =
                createSongError(
                    UNKNOWN_ERROR,
                    "Unknown error with ResponseCode [%s] -- Reason: %s, Message: %s",
                    httpRetryException.responseCode(),
                    httpRetryException.getReason(),
                    httpRetryException.getMessage());
          }
        } else {
          songError = createSongError(UNKNOWN_ERROR, "Unknown error: %s", e.getMessage());
        }
        command.err(errorStatusHeader.getSongClientErrorOutput(songError));
      } else {
        command.err(
            errorStatusHeader.createMessage(
                "The SONG server may not be running on '%s'", config.getServerUrl()));
      }
    } catch (ServerException ex) {
      val songError = ex.getSongError();
      command.err(errorStatusHeader.getSongServerErrorOutput(songError));
    } catch (IOException e) {
      command.err("IO Error: %s", e.getMessage());
    } finally {
      command.report();
    }
  }
}
