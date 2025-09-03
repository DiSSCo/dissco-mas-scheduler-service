package eu.dissco.disscomasschedulerservice.service;

public class ProxyUtils {

  public static final String HANDLE_STRING = "https://hdl.handle.net/";
  public static final String DOI_STRING = "https://doi.org/";

  private ProxyUtils() {
    // Utility class
  }

  public static String removeHandleProxy(String id) {
    return id.replace(HANDLE_STRING, "");
  }

  public static String removeDoiProxy(String id) {
    return id.replace(DOI_STRING, "");
  }

}
