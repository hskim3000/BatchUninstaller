package ddiehl.batchuninstaller.applist

import android.content.pm.PackageManager
import ddiehl.batchuninstaller.model.appinfo.IPackageInfo
import ddiehl.batchuninstaller.model.appinfo.IPackageManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*

class MainPresenter(private val packageManager: IPackageManager) {

    companion object {
        private const val ANDROID_PACKAGE_PREFIX = "com.android"
    }

    private var mainView: MainView? = null

    private val appList: ArrayList<AppViewModel> = ArrayList()

    private val subscriptions = CompositeDisposable()

    fun onViewAttached(view: MainView) {
        mainView = view

        if (appList.isEmpty()) {
            loadApplicationData()
        }
    }

    fun onViewDetached(view: MainView) {
        subscriptions.clear()
        mainView = null
    }

    private fun loadApplicationData() {
        mainView?.let { mainView ->
            getApps(packageManager)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        subscriptions.add(it)
                        mainView.showSpinner()
                    }
                    .doFinally { mainView.dismissSpinner() }
                    .subscribe(this::onAppsLoaded, this::onAppsLoadError)
        }
    }

    private fun onAppsLoaded(apps: List<AppViewModel>) {
        mainView?.showApps(apps)
    }

    private fun onAppsLoadError(error: Throwable) {
        Timber.e(error, "Error processing packages")
        mainView?.showToast(error)
    }

    private fun getApps(packageManager: IPackageManager): Observable<List<AppViewModel>> {
        return Observable.defer {
            val packageList: List<IPackageInfo> = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)

            val apps = packageList
                    .filter { pkg -> !pkg.packageName.startsWith(ANDROID_PACKAGE_PREFIX) }
                    .filter { pkg -> packageManager.getLaunchIntentForPackage(pkg.packageName) != null }
                    .map { pkg -> pkg.packageName }
                    .map { name ->
                        val applicationInfo = packageManager.getApplicationInfo(name, 0)
                        val label = packageManager.getApplicationLabel(applicationInfo)
                        AppViewModel(label, name, 0)
                    }

            Collections.shuffle(apps)
            Observable.just(apps)
        }
    }
}