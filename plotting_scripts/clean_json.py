# Some really ugly code here, but it does what it needs to do and that is all that matters.


def replace_inner_quotes(of: str, start_key: str):
    start_idx = of.find(start_key)

    if start_idx == -1:
        return of

    value_start = start_idx + len(start_key)
    end_idx = of.find(",", value_start)

    if end_idx == -1:
        end_idx = of.find("}", value_start)

    expression_value = of[value_start:end_idx]

    quote_indices = [i for i, c in enumerate(expression_value) if c == '"']

    if len(quote_indices) <= 2:
        assert len(quote_indices) == 2
        return of

    modified_expression = "".join(
        "'" if i in quote_indices[1:-1] else c for i, c in enumerate(expression_value)
    )

    return of[:value_start] + modified_expression + of[end_idx:]


def clean_json(s: str) -> str:
    return replace_inner_quotes(replace_inner_quotes(s, '"expression": '), '"error": ')
