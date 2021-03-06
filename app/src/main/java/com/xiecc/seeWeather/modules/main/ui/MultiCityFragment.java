package com.xiecc.seeWeather.modules.main.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.litesuits.orm.db.assit.WhereBuilder;
import com.xiecc.seeWeather.R;
import com.xiecc.seeWeather.base.BaseFragment;
import com.xiecc.seeWeather.common.OrmLite;
import com.xiecc.seeWeather.common.PLog;
import com.xiecc.seeWeather.common.utils.SimpleSubscriber;
import com.xiecc.seeWeather.common.utils.Util;
import com.xiecc.seeWeather.component.RetrofitSingleton;
import com.xiecc.seeWeather.component.RxBus;
import com.xiecc.seeWeather.modules.main.adapter.MultiCityAdapter;
import com.xiecc.seeWeather.modules.main.domain.CityORM;
import com.xiecc.seeWeather.modules.main.domain.MultiUpdate;
import com.xiecc.seeWeather.modules.main.domain.Weather;
import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Created by HugoXie on 16/7/9.
 *
 * Email: Hugo3641@gamil.com
 * GitHub: https://github.com/xcc3641
 * Info:
 */
public class MultiCityFragment extends BaseFragment {

    @Bind(R.id.recyclerview)
    RecyclerView mRecyclerview;
    @Bind(R.id.swiprefresh)
    SwipeRefreshLayout mSwiprefresh;

    private MultiCityAdapter mAdatper;
    private List<Weather> weatherArrayList;

    private View view;

    /**
     * 加载数据操作,在视图创建之前初始化
     */
    @Override
    protected void lazyLoad() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_multicity, container, false);
            ButterKnife.bind(this, view);
            initView();
        }
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RxBus.getDefault().toObserverable(MultiUpdate.class).subscribe(new SimpleSubscriber<MultiUpdate>() {
            @Override
            public void onNext(MultiUpdate multiUpdate) {
                multiLoad();
            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        multiLoad();
    }

    private void initView() {
        weatherArrayList = new ArrayList<>();
        mAdatper = new MultiCityAdapter(getActivity(), weatherArrayList);
        mRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerview.setAdapter(mAdatper);
        mAdatper.setOnMultiCityLongClick(new MultiCityAdapter.onMultiCityLongClick() {
            @Override
            public void longClick(String city) {
                new AlertDialog.Builder(getActivity()).setMessage("是否删除该城市?")
                    .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            OrmLite.getInstance().delete(new WhereBuilder(CityORM.class).where("name=?", city));
                            OrmLite.OrmTest(CityORM.class);
                            mSwiprefresh.setRefreshing(true);
                            multiLoad();
                        }
                    })
                    .show();
            }
        });

        if (mSwiprefresh != null) {
            mSwiprefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    mSwiprefresh.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            multiLoad();
                        }
                    }, 1000);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private void multiLoad() {
        PLog.d("multi——load");
        weatherArrayList.clear();
        Observable.defer(new Func0<Observable<CityORM>>() {
            @Override
            public Observable<CityORM> call() {
                return Observable.from(OrmLite.getInstance().query(CityORM.class));
            }
        }).doOnTerminate(new Action0() {
            @Override
            public void call() {
                mSwiprefresh.setRefreshing(false);
            }
        })
            .map(new Func1<CityORM, String>() {
                @Override
                public String call(CityORM cityORM) {
                    return Util.replaceCity(cityORM.getName());
                }
            })
            .distinct()
            .flatMap(new Func1<String, Observable<Weather>>() {
                @Override
                public Observable<Weather> call(String s) {
                    return RetrofitSingleton.getInstance().fetchWeather(s);
                }
            })
            .subscribe(new Observer<Weather>() {
                @Override
                public void onCompleted() {
                    mAdatper.notifyDataSetChanged();
                    PLog.d("complete" + weatherArrayList.size() + "");
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Weather weather) {
                    weatherArrayList.add(weather);
                }
            });
    }
}
