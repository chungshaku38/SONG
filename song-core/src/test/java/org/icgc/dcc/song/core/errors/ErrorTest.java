package org.icgc.dcc.song.core.errors;

import lombok.val;
import org.icgc.dcc.song.core.exceptions.SongError;
import org.icgc.dcc.song.core.utils.JsonUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.song.core.utils.Debug.getCallingStackTrace;
import static org.springframework.http.HttpStatus.CONFLICT;

public class ErrorTest {

  @Test
  public void testSongErrorJsonParsing(){
    val expectedError  = new SongError();
    expectedError.setDebugMessage("d1");
    expectedError.setErrorId("something.else");
    expectedError.setHttpStatus(CONFLICT);
    expectedError.setMessage("this message");
    expectedError.setRequestUrl("some url");
    expectedError.setStackTraceElementList(getCallingStackTrace());
    expectedError.setTimestamp(System.currentTimeMillis());

    val actualError = JsonUtils.fromJson(expectedError.toJson(), SongError.class);
    assertThat(actualError).isEqualTo(expectedError);

  }

}