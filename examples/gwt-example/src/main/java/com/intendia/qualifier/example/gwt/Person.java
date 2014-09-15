// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.example.gwt;

import com.intendia.qualifier.annotation.Qualify;

@Qualify
public interface Person {
    Integer getId();

    String getName();

    void setName(String name);

    Address getAddress();

    void setAddress(Address address);

}
