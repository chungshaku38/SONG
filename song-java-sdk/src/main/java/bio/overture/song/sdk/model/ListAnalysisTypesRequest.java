package bio.overture.song.sdk.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ListAnalysisTypesRequest {
  @NonNull private final List<String> names;
  @NonNull private final List<Integer> versions;
  @NonNull private final List<SortOrder> sortOrders;

  /** Nullable */
  private final Integer offset;

  private final Integer limit;
  private final Boolean hideSchema;
  private final Boolean unrenderedOnly;
  private final SortDirection sortDirection;
}