package com.bayoumi.models.azkar;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbsoluteZekrDTO {

    private String zekr;
    private String uuid;

    @JsonProperty("text")

    public String getZekr() {
        return zekr;
    }

    @JsonProperty("text")

    public void setZekr(String zekr) {
        this.zekr = zekr;
    }

    @JsonProperty("uuid")

    public String getUuid() {
        return uuid;
    }

    @JsonProperty("uuid")

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AbsoluteZekrDTO that = (AbsoluteZekrDTO) o;
        return Objects.equals(zekr, that.zekr) || Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(zekr);
    }
}
