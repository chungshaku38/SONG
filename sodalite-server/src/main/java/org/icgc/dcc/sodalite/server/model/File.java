
package org.icgc.dcc.sodalite.server.model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
		"objectId",
    "fileName",
    "fileSize",
    "fileType",
    "fileMd5"
})
public class File {

		@JsonProperty("objectId")
		private String objectId;
  
    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileSize")
    private int fileSize;

    @JsonProperty("fileType")
    private FileType fileType;

    @JsonProperty("fileMd5")
    private String fileMd5;
    
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("fileName")
    public String getFileName() {
        return fileName;
    }

    @JsonProperty("fileName")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public File withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @JsonProperty("fileSize")
    public int getFileSize() {
        return fileSize;
    }

    @JsonProperty("fileSize")
    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public File withFileSize(int fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    @JsonProperty("fileType")
    public FileType getFileType() {
        return fileType;
    }

    @JsonProperty("fileType")
    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public File withFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    @JsonProperty("fileMd5")
    public String getFileMd5() {
        return fileMd5;
    }

    @JsonProperty("fileMd5")
    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public File withFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public File withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }
}