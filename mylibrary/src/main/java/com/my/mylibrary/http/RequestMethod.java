package com.my.mylibrary.http;


import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({RequestMethod.GET, RequestMethod.POST})
@Retention(RetentionPolicy.SOURCE)
public @interface RequestMethod {
    String GET = "GET";
    String POST = "POST";
}
