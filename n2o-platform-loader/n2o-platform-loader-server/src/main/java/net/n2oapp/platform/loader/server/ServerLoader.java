package net.n2oapp.platform.loader.server;

/**
 * Серверный загрузчик
 *
 * @param <T> Тип данных
 */
public interface ServerLoader<T> {
    /**
     * Загрузить данные
     *
     * @param data    Данные
     * @param subject Субъект
     */
    void load(T data, String subject);
}
