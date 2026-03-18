package dbcomponent;

/**
 * Identificador tipado para queries predefinidas.
 *
 * La app NO debería ejecutar SQL crudo; en su lugar usa un ID que se resuelve a un SQL
 * cargado desde un archivo de propiedades interno del componente.
 */
public record DBQueryId(String value) {
    public DBQueryId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DBQueryId no puede ser null/vacío");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

