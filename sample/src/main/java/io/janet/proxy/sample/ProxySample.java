package io.janet.proxy.sample;

import com.google.gson.Gson;

import io.janet.HttpActionService;
import io.janet.Janet;
import io.janet.ProxyService;
import io.janet.gson.GsonConverter;
import io.janet.http.annotations.HttpAction;
import io.janet.okhttp.OkClient;
import io.janet.proxy.sample.actions.GithubAction;
import io.janet.proxy.sample.actions.XkcdAction;
import io.janet.proxy.sample.actions.base.LabeledAction;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public class ProxySample {

  public static void main(String... args) throws Throwable {
    OkClient client = new OkClient();
    GsonConverter converter = new GsonConverter(new Gson());

    Janet janet = new Janet.Builder()
        .addService(new SampleLoggingService(new ProxyService.Builder(HttpAction.class)
            .add(
                new HttpActionService("https://api.github.com", client, converter),
                action -> ((LabeledAction) action).label().equals("github"))
            .add(
                new HttpActionService("http://xkcd.com", client, converter),
                action -> ((LabeledAction) action).label().equals("xkcd"))
            .build()
        )).build();

    Flowable.combineLatest(
        janet.createPipe(GithubAction.class, Schedulers.io()).createObservableResult(new GithubAction()).toFlowable(),
        janet.createPipe(XkcdAction.class, Schedulers.io()).createObservableResult(new XkcdAction()).toFlowable(),
        (githubAction, xkcdAction) -> "Success!"
    ).blockingFirst();
  }
}

