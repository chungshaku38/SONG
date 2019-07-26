package bio.overture.song.server.utils.web;

import bio.overture.song.core.utils.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.icgc.dcc.common.core.util.Joiners.PATH;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static bio.overture.song.server.utils.CollectionUtils.isArrayBlank;
import static bio.overture.song.server.utils.CollectionUtils.isCollectionBlank;
import static bio.overture.song.server.utils.EndpointTester.AMPERSAND;
import static bio.overture.song.server.utils.web.QueryParam.createQueryParam;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractWebResource<
    T, O extends ResponseOption<T, O>, W extends AbstractWebResource<T, O, W>> {

  private static final ObjectMapper REGULAR_MAPPER = new ObjectMapper();
  private static final ObjectMapper PRETTY_MAPPER = new ObjectMapper();

  static {
    PRETTY_MAPPER.enable(INDENT_OUTPUT);
  }

  @NonNull private final MockMvc mockMvc;
  @NonNull private final String serverUrl;
  @NonNull private final Class<T> responseType;

  private String endpoint;
  private Set<QueryParam> queryParams = newHashSet();
  private Object body;
  private HttpHeaders headers;
  private boolean enableLogging = false;
  private boolean pretty = false;

  protected abstract O createResponseOption(ResponseEntity<T> responseEntity);

  private W thisInstance() {
    return (W) this;
  }

  public W endpoint(String formattedEndpoint, Object... args) {
    this.endpoint = format(formattedEndpoint, args);
    return thisInstance();
  }

  public W body(Object body) {
    this.body = body;
    return thisInstance();
  }

  public W headers(HttpHeaders httpHeaders) {
    this.headers = httpHeaders;
    return thisInstance();
  }

  public W logging() {
    return configLogging(true, false);
  }

  public W prettyLogging() {
    return configLogging(true, true);
  }

  public W optionalQueryParamCollection(String key, Collection values) {
    if(!isCollectionBlank(values)){
      return queryParam(key, values);
    }
    return thisInstance();
  }

  public W optionalQuerySingleParam(String key, Object value) {
    if(!isNull(value)){
      return querySingleParam(key, value);
    }
    return thisInstance();
  }

  public W querySingleParam(String key, Object value) {
    return queryParam(key, ImmutableList.of(value));
  }

  public W optionalQueryParamArray(String key, Object[] values) {
    if(!isArrayBlank(values)){
      return optionalQueryParamCollection(key, newArrayList(values));
    }
    return thisInstance();
  }

  public W optionalQueryParamMulti(String key, Object ... values) {
    if(!isArrayBlank(values)){
      return optionalQueryParamCollection(key, newArrayList(values));
    }
    return thisInstance();
  }

  public W queryParam(String key, Collection values) {
    queryParams.add(createQueryParam(key, values));
    return thisInstance();
  }

  private W configLogging(boolean enable, boolean pretty) {
    this.enableLogging = enable;
    this.pretty = pretty;
    return thisInstance();
  }

  public ResponseEntity<T> get() {
    return doRequest(null, HttpMethod.GET);
  }

  public ResponseEntity<T> put() {
    return doRequest(this.body, HttpMethod.PUT);
  }

  public ResponseEntity<T> post() {
    return doRequest(this.body, HttpMethod.POST);
  }

  public ResponseEntity<T> delete() {
    return doRequest(null, HttpMethod.DELETE);
  }

  public O deleteAnd() {
    return createResponseOption(delete());
  }

  public O getAnd() {
    return createResponseOption(get());
  }

  public O putAnd() {
    return createResponseOption(put());
  }

  public O postAnd() {
    return createResponseOption(post());
  }

  private Optional<String> getQuery() {
    val queryStrings = queryParams.stream().map(QueryParam::toString).collect(toImmutableSet());
    return queryStrings.isEmpty() ? Optional.empty() : Optional.of(AMPERSAND.join(queryStrings));
  }

  private String getUrl() {
    return PATH.join(this.serverUrl, this.endpoint) + getQuery().map(x -> "?" + x).orElse("");
  }

  @SneakyThrows
  private ResponseEntity<T> doRequest(Object body, HttpMethod httpMethod) {
    logRequest(enableLogging, pretty, httpMethod, getUrl(), body);
    val mvcResponse = mockMvc.perform(MockMvcRequestBuilders.request(httpMethod,getUrl())).andReturn().getResponse();

    val responseObject = responseType == String.class ? (T)mvcResponse.getContentAsString() : JsonUtils.fromJson(mvcResponse.getContentAsString(), this.responseType);
    val response = ResponseEntity.status(mvcResponse.getStatus()).body(responseObject);
    logResponse(enableLogging, pretty, response);
    return response;
  }

  @SneakyThrows
  private static void logRequest(
      boolean enable, boolean pretty, HttpMethod httpMethod, String url, Object body) {
    if (enable) {
      if (isNull(body)) {
        log.info("[REQUEST] {} {}", httpMethod, url);
      } else {
        if (pretty) {
          log.info(
              "[REQUEST] {} {} < \n{}", httpMethod, url, PRETTY_MAPPER.writeValueAsString(body));
        } else {
          log.info(
              "[REQUEST] {} {} < {}", httpMethod, url, REGULAR_MAPPER.writeValueAsString(body));
        }
      }
    }
  }

  @SneakyThrows
  private static <T> void logResponse(boolean enable, boolean pretty, ResponseEntity<T> response) {
    if (enable) {
      val output =
          CleanResponse.builder()
              .body(response.hasBody() ? response.getBody() : null)
              .statusCodeName(response.getStatusCode().name())
              .statusCodeValue(response.getStatusCodeValue())
              .build();
      if (pretty) {
        log.info("[RESPONSE] > \n{}", PRETTY_MAPPER.writeValueAsString(output));
      } else {
        log.info("[RESPONSE] > {}", REGULAR_MAPPER.writeValueAsString(output));
      }
    }
  }
}
