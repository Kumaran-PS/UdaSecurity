module com.udacity.catpoint.security {
    requires miglayout;
    requires java.desktop;
    requires com.google.gson;
    requires java.prefs;
    requires transitive com.udacity.catpoint.image;
    requires com.google.common;
    opens com.udacity.catpoint.security.data to com.google.gson;
}