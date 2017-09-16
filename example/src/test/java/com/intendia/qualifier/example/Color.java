package com.intendia.qualifier.example;

import com.intendia.qualifier.annotation.Qualify;
import java.util.Objects;

@Qualify public class Color {
    private final String name;

    public Color(String name) { this.name = name; }

    public String getName() { return name; }

    @Override public boolean equals(Object o) { return this == o || o instanceof Color && equals((Color) o); }

    public boolean equals(Color o) { return Objects.equals(name, o.name); }

    @Override public int hashCode() { return Objects.hash(name); }

    public static Color valueOf(String color) { return new Color(color); }
}
