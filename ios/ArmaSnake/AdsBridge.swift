import UIKit
import ArmaShared
import GoogleMobileAds
import UserMessagingPlatform

/// Google Mobile Ads (iOS) behind the Kotlin `AdBridge` interface: UMP consent first,
/// one interstitial and one rewarded ad kept preloaded, stale callbacks dropped.
final class AdsBridge: NSObject, AdBridge, FullScreenContentDelegate {
    private let interstitialUnit: String
    private let rewardedUnit: String
    private var interstitial: InterstitialAd?
    private var rewarded: RewardedAd?
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var session = 0
    private var started = false

    private var onInterstitialDone: (() -> Void)?
    private var onRewardClosed: (() -> Void)?
    private var onRewardUnavailable: (() -> Void)?

    override init() {
        let info = Bundle.main.infoDictionary ?? [:]
        interstitialUnit = info["ArmaInterstitialAdUnit"] as? String ?? ""
        rewardedUnit = info["ArmaRewardedAdUnit"] as? String ?? ""
        super.init()
    }

    // MARK: consent

    func start(from host: UIViewController) {
        ConsentInformation.shared.requestConsentInfoUpdate(with: RequestParameters()) { [weak self] _ in
            ConsentForm.loadAndPresentIfRequired(from: host) { _ in
                self?.consentChanged()
            }
        }
        consentChanged()
    }

    func showPrivacyOptions(from host: UIViewController) {
        invalidate()
        ConsentForm.presentPrivacyOptionsForm(from: host) { [weak self] _ in
            self?.consentChanged()
        }
    }

    private func consentChanged() {
        let can = ConsentInformation.shared.canRequestAds
        IosAppState.shared.canRequestAds = can
        IosAppState.shared.privacyOptionsRequired =
            ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        guard can else { invalidate(); return }
        if !started {
            started = true
            MobileAds.shared.start { [weak self] _ in
                DispatchQueue.main.async { self?.preload() }
            }
        } else {
            preload()
        }
    }

    private func invalidate() {
        session += 1
        interstitial = nil
        rewarded = nil
        loadingInterstitial = false
        loadingRewarded = false
    }

    // MARK: AdBridge

    func isRewardedReady() -> Bool { rewarded != nil }
    func isInterstitialReady() -> Bool { interstitial != nil }
    func isLoading(rewarded wantRewarded: Bool) -> Bool { wantRewarded ? loadingRewarded : loadingInterstitial }

    func preload() {
        guard started, ConsentInformation.shared.canRequestAds else { return }
        loadInterstitial()
        loadRewarded()
    }

    func showInterstitial(onDone: @escaping () -> KotlinUnit) {
        guard let ad = interstitial, let host = topController() else {
            loadInterstitial()
            _ = onDone()
            return
        }
        interstitial = nil
        onInterstitialDone = { _ = onDone() }
        ad.present(from: host)
    }

    func showRewarded(onReward: @escaping () -> KotlinUnit,
                      onUnavailable: @escaping () -> KotlinUnit,
                      onClosed: @escaping () -> KotlinUnit) {
        guard let ad = rewarded, let host = topController() else {
            loadRewarded()
            _ = onUnavailable()
            return
        }
        rewarded = nil
        onRewardClosed = { _ = onClosed() }
        onRewardUnavailable = { _ = onUnavailable() }
        ad.present(from: host) {
            _ = onReward()
        }
    }

    // MARK: loading

    private func loadInterstitial() {
        guard interstitial == nil, !loadingInterstitial, !interstitialUnit.isEmpty else { return }
        loadingInterstitial = true
        let s = session
        InterstitialAd.load(with: interstitialUnit, request: Request()) { [weak self] ad, _ in
            DispatchQueue.main.async {
                guard let self, s == self.session else { return }
                self.loadingInterstitial = false
                self.interstitial = ad
                ad?.fullScreenContentDelegate = self
            }
        }
    }

    private func loadRewarded() {
        guard rewarded == nil, !loadingRewarded, !rewardedUnit.isEmpty else { return }
        loadingRewarded = true
        let s = session
        RewardedAd.load(with: rewardedUnit, request: Request()) { [weak self] ad, _ in
            DispatchQueue.main.async {
                guard let self, s == self.session else { return }
                self.loadingRewarded = false
                self.rewarded = ad
                ad?.fullScreenContentDelegate = self
            }
        }
    }

    // MARK: FullScreenContentDelegate

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) { finish(ad, failed: false) }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        finish(ad, failed: true)
    }

    private func finish(_ ad: FullScreenPresentingAd, failed: Bool) {
        if ad is InterstitialAd {
            let cb = onInterstitialDone
            onInterstitialDone = nil
            loadInterstitial()
            cb?()
        } else if ad is RewardedAd {
            let closed = onRewardClosed, unavailable = onRewardUnavailable
            onRewardClosed = nil
            onRewardUnavailable = nil
            loadRewarded()
            if failed { unavailable?() } else { closed?() }
        }
    }

    private func topController() -> UIViewController? {
        var vc = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }.first?.rootViewController
        while let presented = vc?.presentedViewController { vc = presented }
        return vc
    }
}
