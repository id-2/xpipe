## Конфигурации SSH

Здесь ты можешь указать любые опции SSH, которые должны быть переданы соединению.
Хотя некоторые опции по сути являются обязательными для успешного установления соединения, например `HostName`,
многие другие опции являются чисто необязательными.

Чтобы получить общее представление обо всех возможных опциях, ты можешь использовать [`man ssh_config`](https://linux.die.net/man/5/ssh_config) или прочитать это [руководство](https://www.ssh.com/academy/ssh/config).
Точное количество поддерживаемых опций зависит исключительно от установленного у тебя SSH-клиента.

### Форматирование

Приведенное здесь содержимое эквивалентно одной секции хоста в конфигурационном файле SSH.
Обрати внимание, что тебе не нужно явно определять ключ `Host`, так как это будет сделано автоматически.

Если ты собираешься определить более одного раздела хоста, например, с зависимыми соединениями, такими как прокси-хост прыжка, который зависит от другого конфиг-хоста, ты можешь определить несколько записей хоста и здесь. После этого XPipe запустит первую запись хоста.

Не нужно выполнять никакого форматирования с пробелами или отступами, это не требуется для работы.

Обрати внимание, что ты должен позаботиться о том, чтобы заключить в кавычки все значения, если они содержат пробелы, иначе они будут переданы неверно.

### Файлы идентификации

Обрати внимание, что ты также можешь указать здесь опцию `IdentityFile`.
Если эта опция указана здесь, то любая другая опция аутентификации на основе ключа, указанная ниже, будет проигнорирована.

Если ты решил обратиться к файлу идентификации, который находится в git-хранилище XPipe, ты можешь сделать и это.
XPipe обнаружит общие файлы идентификации и автоматически адаптирует путь к файлу на каждой системе, на которой ты клонировал git-хранилище.