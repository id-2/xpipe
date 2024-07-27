package io.xpipe.ext.base.script;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.ext.base.browser.MultiExecuteAction;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RunScriptAction implements BrowserAction, BranchAction {

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2c-code-greater-than");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("runScript");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var sc = model.getFileSystem().getShell().orElseThrow();
        return model.getBrowserModel() instanceof BrowserSessionModel
                && !getInstances(sc).isEmpty();
    }

    private Map<String, SimpleScriptStore> getInstances(ShellControl sc) {
        var scripts = ScriptStore.flatten(ScriptStore.getDefaultEnabledScripts());
        var map = new LinkedHashMap<String, SimpleScriptStore>();
        for (SimpleScriptStore script : scripts) {
            if (!script.isFileScript()) {
                continue;
            }

            if (!script.isCompatible(sc)) {
                continue;
            }

            var entry = DataStorage.get().getStoreEntryIfPresent(script, true);
            if (entry.isPresent()) {
                map.put(entry.get().getName(), script);
            }
        }
        return map;
    }

    @Override
    public List<BranchAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var sc = model.getFileSystem().getShell().orElseThrow();
        var scripts = getInstances(sc);
        List<BranchAction> actions = scripts.entrySet().stream()
                .map(e -> {
                    return new MultiExecuteAction() {

                        @Override
                        public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                            return new SimpleStringProperty(e.getKey());
                        }

                        @Override
                        protected CommandBuilder createCommand(ShellControl sc, OpenFileSystemModel model, BrowserEntry entry) {
                            if (!(model.getBrowserModel() instanceof BrowserSessionModel bm)) {
                                return null;
                            }

                            var content = e.getValue().assemble(sc);
                            var script = ScriptHelper.createExecScript(sc, content);
                            var builder = CommandBuilder.of().add(sc.getShellDialect().runScriptCommand(sc, script.toString()));
                            entries.stream()
                                    .map(browserEntry -> browserEntry
                                            .getRawFileEntry()
                                            .getPath())
                                    .forEach(s -> {
                                        builder.addFile(s);
                                    });
                            return builder;
                        }
                    };
                })
                .map(leafAction -> (BranchAction) leafAction)
                .toList();
        return actions;
    }
}
