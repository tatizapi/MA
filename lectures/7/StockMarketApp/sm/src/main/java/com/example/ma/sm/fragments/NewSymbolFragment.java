package com.example.ma.sm.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ma.sm.R;
import com.example.ma.sm.StockApp;
import com.example.ma.sm.model.Symbol;
import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.Date;

import javax.annotation.Nullable;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func3;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;

public class NewSymbolFragment extends BaseFragment {
  @BindView(R.id.new_symbol_indicator)
  TextView validIndicator;
  @BindView(R.id.new_symbol_name)
  EditText name;
  @BindView(R.id.new_symbol_quantity)
  EditText quantity;
  @BindView(R.id.new_symbol_price)
  EditText price;

  private Observable<CharSequence> nameChangeObservable;
  private Observable<CharSequence> quantityChangeObservable;
  private Observable<CharSequence> priceChangeObservable;

  private Subscription subscription = null;
  private long portfolioId;
  private boolean valid;

  @Inject
  Realm realm;

  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.new_symbol, container, false);
    ButterKnife.bind(this, layout);
    StockApp.get().injector().inject(this);

    portfolioId = getArguments().getLong("portfolioId");

    nameChangeObservable = RxTextView.textChanges(name).skip(1);
    quantityChangeObservable = RxTextView.textChanges(quantity).skip(1);
    priceChangeObservable = RxTextView.textChanges(price).skip(1);
    valid = false;
    combineEvents();

    return layout;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    subscription.unsubscribe();
  }

  private void combineEvents() {
    subscription = Observable.combineLatest(nameChangeObservable,
        quantityChangeObservable,
        priceChangeObservable,
        new Func3<CharSequence, CharSequence, CharSequence, Boolean>() {
          @Override
          public Boolean call(CharSequence newName, CharSequence newQuantity, CharSequence newPrice) {
            boolean nameValid = !isEmpty(newName);
            if (!nameValid) {
              name.setError("Invalid Name!");
            }
            boolean quantityValid = !isEmpty(newQuantity);
            if (quantityValid) {
              int num = Integer.parseInt(newQuantity.toString());
              quantityValid = num > 1 && num <= 100;
            }
            if (!quantityValid) {
              quantity.setError("Invalid quantity!");
            }
            boolean priceValid = !isEmpty(newPrice);
            if (priceValid) {
              double num = Double.parseDouble(newPrice.toString());
              priceValid = num > 0 && num <= 50;
            }
            if (!priceValid) {
              price.setError("Invalid Price!");
            }
            return nameValid && quantityValid && priceValid;
          }
        })
        .subscribe(new Observer<Boolean>() {
          @Override
          public void onCompleted() {
            Timber.d("completed");
          }

          @Override
          public void onError(Throwable e) {
            Timber.e(e, "there was an error");
          }

          @Override
          public void onNext(Boolean formValid) {
            int color = ContextCompat.getColor(getContext(), R.color.colorPrimary);
            if (!formValid) {
              valid = false;
              color = ContextCompat.getColor(getContext(), R.color.colorAccent);
            } else {
              valid = true;
            }
            validIndicator.setBackgroundColor(color);
          }
        });
  }

  @OnClick(R.id.new_symbol_indicator)
  public void saveSymbol() {
    if (valid) {
      realm.beginTransaction();
      Symbol symbol = new Symbol();
      symbol.setName(name.getText().toString());
      symbol.setQuantity(Long.parseLong(quantity.getText().toString()));
      symbol.setAcquisitionPrice(Double.parseDouble(price.getText().toString()));
      symbol.setAcquisitionDate(new Date());
      StockApp.get().getManager().addSymbol(portfolioId, symbol);
      Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
      realm.commitTransaction();
      getFragmentManager().popBackStack();
    } else {
      Toast.makeText(getContext(), "Not able to save!", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    realm.close();
  }
}
