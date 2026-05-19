import os

def resolve_base_activity():
    path = 'app/src/main/java/com/github/tvbox/osc/base/BaseActivity.java'
    with open(path, 'r') as f:
        lines = f.readlines()
    output = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith('<<<<<<< HEAD'):
            i += 1
            head = []
            while not lines[i].startswith('======='):
                head.append(lines[i])
                i += 1
            i += 1 # skip =======
            main = []
            while not lines[i].startswith('>>>>>>> origin/main'):
                main.append(lines[i])
                i += 1
            i += 1 # skip >>>>>>>
            # Resolution: HEAD has single tab, main has double tab. Prefer main's logic if identical but here just normalize.
            # Actually, I'll just use the one I manually verified.
            output.extend(head)
        else:
            output.append(line)
            i += 1
    with open(path, 'w') as f:
        f.writelines(output)

def resolve_vod_controller():
    path = 'app/src/main/java/com/github/tvbox/osc/player/controller/VodController.kt'
    with open(path, 'r') as f:
        lines = f.readlines()
    output = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith('<<<<<<< HEAD'):
            i += 1
            head = []
            while not lines[i].startswith('======='):
                head.append(lines[i])
                i += 1
            i += 1
            main = []
            while not lines[i].startswith('>>>>>>> origin/main'):
                main.append(lines[i])
                i += 1
            i += 1

            content_head = "".join(head)
            if 'import' in content_head:
                res = head
                if 'import com.github.tvbox.osc.ui.activity.DetailActivity' in "".join(main):
                    res.append('import com.github.tvbox.osc.ui.activity.DetailActivity\n')
            elif 'App.getInstance()' in content_head:
                res = head
            elif 'simpleName == "PlayActivity"' in content_head:
                res = [
                    '                    if (mActivity?.javaClass?.simpleName == "PlayActivity") {\n',
                    '                        // (mActivity as? PlayActivity)?.toggleFullPreview()\n',
                    '                    } else if (mActivity?.javaClass?.simpleName == "DetailActivity") {\n',
                    '                        (mActivity as? DetailActivity)?.toggleFullPreview()\n',
                    '                    } else {\n',
                    '                        mActivity?.finish()\n',
                    '                    }\n'
                ]
            else:
                res = head
            output.extend(res)
        else:
            output.append(line)
            i += 1
    with open(path, 'w') as f:
        f.writelines(output)

def resolve_play_activity():
    path = 'app/src/main/java/com/github/tvbox/osc/ui/activity/PlayActivity.kt'
    with open(path, 'r') as f:
        lines = f.readlines()
    output = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith('<<<<<<< HEAD'):
            i += 1
            head = []
            while not lines[i].startswith('======='):
                head.append(lines[i])
                i += 1
            i += 1
            main = []
            while not lines[i].startswith('>>>>>>> origin/main'):
                main.append(lines[i])
                i += 1
            i += 1
            output.extend(head)
        else:
            output.append(line)
            i += 1
    with open(path, 'w') as f:
        f.writelines(output)

def resolve_play_fragment():
    path = 'app/src/main/java/com/github/tvbox/osc/ui/fragment/PlayFragment.java'
    with open(path, 'r') as f:
        lines = f.readlines()
    output = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith('<<<<<<< HEAD'):
            i += 1
            head = []
            while not lines[i].startswith('======='):
                head.append(lines[i])
                i += 1
            i += 1
            main = []
            while not lines[i].startswith('>>>>>>> origin/main'):
                main.append(lines[i])
                i += 1
            i += 1
            output.extend(head)
        else:
            output.append(line)
            i += 1
    with open(path, 'w') as f:
        f.writelines(output)

resolve_base_activity()
resolve_vod_controller()
resolve_play_activity()
resolve_play_fragment()
