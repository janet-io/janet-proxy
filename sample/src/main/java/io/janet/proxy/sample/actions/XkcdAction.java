package io.janet.proxy.sample.actions;

import io.janet.http.annotations.HttpAction;
import io.janet.http.annotations.Response;
import io.janet.proxy.sample.actions.base.LabeledAction;

@HttpAction("/info.0.json")
public class XkcdAction implements LabeledAction {

  @Override public String label() {
    return "xkcd";
  }

  @Response XkcdData data;

  public XkcdData getData() {
    return data;
  }

  @Override public String toString() {
    return "XkcdAction{" +
        "data=" + data +
        '}';
  }

  public static class XkcdData {

    public final String img;
    public final String title;
    public final String alt;
    public final String num;

    public XkcdData(String img, String title, String alt, String num) {
      this.img = img;
      this.title = title;
      this.alt = alt;
      this.num = num;
    }

    @Override public String toString() {
      return "XkcdData{" +
          "img='" + img + '\'' +
          ", title='" + title + '\'' +
          ", alt='" + alt + '\'' +
          ", num='" + num + '\'' +
          '}';
    }
  }
}
