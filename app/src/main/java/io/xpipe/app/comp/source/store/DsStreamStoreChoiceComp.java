package io.xpipe.app.comp.source.store;

import io.xpipe.app.comp.base.FileDropOverlayComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataSourceProviders;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import io.xpipe.extension.fxcomps.impl.TabPaneComp;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import io.xpipe.extension.util.SimpleValidator;
import io.xpipe.extension.util.Validatable;
import io.xpipe.extension.util.Validator;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.synedra.validatorfx.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Value
@EqualsAndHashCode(callSuper = true)
public class DsStreamStoreChoiceComp extends SimpleComp implements Validatable {

    public static enum Mode {
        OPEN,
        WRITE
    }

    Property<DataStore> selected;
    Property<DataSourceProvider<?>> provider;
    boolean showAnonymous;
    boolean showSaved;
    Validator validator;
    Check check;
    DsStreamStoreChoiceComp.Mode mode;

    public DsStreamStoreChoiceComp(
            Property<DataStore> selected,
            Property<DataSourceProvider<?>> provider,
            boolean showAnonymous,
            boolean showSaved,
            Mode mode) {
        this.selected = selected;
        this.provider = provider;
        this.showAnonymous = showAnonymous;
        this.showSaved = showSaved;
        this.mode = mode;
        validator = new SimpleValidator();
        check = Validator.nonNull(validator, I18n.observable("streamStore"), selected);
    }

    @Override
    protected Region createSimple() {
        var isNamedStore =
                XPipeDaemon.getInstance().getStoreName(selected.getValue()).isPresent();
        var localStore = new SimpleObjectProperty<DataStore>(
                !isNamedStore
                                && selected.getValue() instanceof FileStore fileStore
                                && fileStore.getFileSystem() instanceof LocalStore
                        ? selected.getValue()
                        : null);
        var browseComp = new DsLocalFileBrowseComp(provider, localStore, mode).apply(GrowAugment.create(true, false));
        var dragAndDropLabel = Comp.of(() -> new Label(I18n.get("dragAndDropFilesHere")))
                .apply(s -> s.get().setAlignment(Pos.CENTER))
                .apply(struc -> AppFont.small(struc.get()));
        // var historyComp = new DsFileHistoryComp(provider, chosenFile);
        var local = new TabPaneComp.Entry(
                I18n.observable("localFile"),
                "mdi2m-monitor",
                new VerticalComp(List.of(browseComp, dragAndDropLabel))
                        .styleClass("store-local-file-chooser")
                        .apply(s -> s.get().setFillWidth(true))
                        .apply(s -> s.get().setSpacing(30))
                        .apply(s -> s.get().setAlignment(Pos.TOP_CENTER)));

        var filter = Bindings.createObjectBinding(
                () -> (Predicate<DataStoreEntry>) e -> {
                    if (provider == null || provider.getValue() == null) {
                        return e.getStore() instanceof StreamDataStore;
                    }

                    return provider.getValue().couldSupportStore(e.getStore());
                },
                provider != null ? provider : new SimpleObjectProperty<>());

        var remoteStore = new SimpleObjectProperty<DataStore>(
                isNamedStore
                                && selected.getValue() instanceof FileStore fileStore
                                && !(fileStore.getFileSystem() instanceof LocalStore)
                        ? selected.getValue()
                        : null);
        var remote = new TabPaneComp.Entry(
                I18n.observable("remote"), "mdi2e-earth", new DsRemoteFileChoiceComp(remoteStore));

        var namedStore = new SimpleObjectProperty<DataStore>(isNamedStore ? selected.getValue() : null);
        var named = new TabPaneComp.Entry(
                I18n.observable("stored"),
                "mdrmz-storage",
                NamedStoreChoiceComp.create(filter, namedStore, DataStoreProvider.DataCategory.STREAM));

        var otherStore = new SimpleObjectProperty<DataStore>(
                localStore.get() == null && remoteStore.get() == null && !isNamedStore ? selected.getValue() : null);
        var other = new TabPaneComp.Entry(
                I18n.observable("other"),
                "mdrmz-web_asset",
                new DataStoreSelectorComp(DataStoreProvider.DataCategory.STREAM, otherStore));

        var selectedTab = new SimpleObjectProperty<TabPaneComp.Entry>();
        if (localStore.get() != null) {
            selectedTab.set(local);
        } else if (remoteStore.get() != null) {
            selectedTab.set(remote);
        } else if (namedStore.get() != null) {
            selectedTab.set(named);
        } else if (otherStore.get() != null) {
            selectedTab.set(other);
        } else {
            selectedTab.set(local);
        }

        selected.addListener((observable, oldValue, newValue) -> {
            if (provider != null && provider.getValue() == null) {
                provider.setValue(
                        DataSourceProviders.byPreferredStore(newValue, null).orElse(null));
            }
        });

        SimpleChangeListener.apply(selectedTab, c -> {
            if (c == local) {
                this.selected.bind(localStore);
            }
            if (c == remote) {
                this.selected.bind(remoteStore);
            }
            if (c == named) {
                this.selected.bind(namedStore);
            }
            if (c == other) {
                this.selected.bind(otherStore);
            }
        });
        var entries = new ArrayList<>(List.of(local, remote));
        if (showSaved) {
            entries.add(named);
        }
        if (showAnonymous) {
            entries.add(other);
        }

        var pane = new TabPaneComp(selectedTab, entries);

        pane.apply(s -> AppFont.normal(s.get()));

        var fileDrop = new FileDropOverlayComp<>(pane, files -> {
            if (files.size() != 1) {
                return;
            }

            var f = files.get(0);
            var store = FileStore.local(f);
            selectedTab.set(local);
            localStore.set(store);
        });

        var region = fileDrop.createRegion();
        check.decorates(region);
        return region;
    }
}
