package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Dto {

    @JsonIgnore
    Type getDtoType();

    interface Type {

    }
}
