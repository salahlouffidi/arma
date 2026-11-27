import UIKit
import ArmaShared

/// Hosts the shared Compose game (ArmaShared.framework) full screen, in landscape,
/// and wires AdMob + UMP consent into the Kotlin side.
final class GameHostController: UIViewController {
    private let ads = AdsBridge()

    override var prefersStatusBarHidden: Bool { true }
    override var prefersHomeIndicatorAutoHidden: Bool { true }
    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge { .all }
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        UIDevice.current.userInterfaceIdiom == .pad ? .all : .landscape
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black

        IosAds.shared.bridge = ads
        IosAppState.shared.openPrivacyOptions = { [weak self] in
            if let self { self.ads.showPrivacyOptions(from: self) }
        }

        let game = MainViewControllerKt.MainViewController()
        addChild(game)
        game.view.frame = view.bounds
        game.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(game.view)
        game.didMove(toParent: self)
    }

    private var started = false
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        setNeedsUpdateOfScreenEdgesDeferringSystemGestures()
        guard !started else { return }
        started = true
        ads.start(from: self)
    }
}
