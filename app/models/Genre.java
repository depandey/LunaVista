package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.EnumValue;

/**
 * Created by 609108084 on 03/04/2016.
 */
public enum Genre {

    @EnumValue("Alternative")ALTERNATIVE, @EnumValue("Anime")ANIME, @EnumValue("Ballads")BALLADS,

    @EnumValue("Blues")BLUES, @EnumValue("Bolero")BOLERO, @EnumValue("Brazilian")BRAZILIAN, @EnumValue("Traditional")TRADITIONAL,

    @EnumValue("Broadway")BROADWAY, @EnumValue("Song Writers")SONGWRITERS, @EnumValue("Celtic")CELTIC,

    @EnumValue("Christian")CHRISTIAN, @EnumValue("Classical")CLASSICAL, @EnumValue("Country")COUNTRY;
}