package com.intendia.qualifier.example.gwt;// Copyright 2014 Intendia, SL.

import com.intendia.qualifier.annotation.Qualify;

@Qualify
public interface Address {
    String getStreet();

    Integer getNumber();

    String getCity();
}
