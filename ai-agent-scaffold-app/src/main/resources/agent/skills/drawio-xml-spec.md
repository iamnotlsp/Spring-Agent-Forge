# drawio-xml-spec

Use this skill when generating, repairing, or normalizing Draw.io XML.

## Output Contract

- Return JSON only when the caller requests JSON.
- For Draw.io generation, the JSON shape must be:
  `{"type":"drawio","content":"FULL_MXFILE_XML"}`
- Do not wrap the JSON or XML in markdown fences.
- Do not add explanations before or after the JSON.
- The `content` value must be one complete `<mxfile>` document.

## Required Draw.io Structure

Every generated Draw.io XML document must include:

- `<mxfile host="embed.diagrams.net" type="embed">`
- at least one `<diagram id="..." name="...">`
- one `<mxGraphModel ...>`
- one `<root>`
- `<mxCell id="0" />`
- `<mxCell id="1" parent="0" />`

## Node Rules

- Every visible node must be an `<mxCell>` with `vertex="1"` and `parent="1"` or a valid group parent.
- Every visible node must include an `<mxGeometry x="..." y="..." width="..." height="..." as="geometry" />`.
- Use unique and stable ids. Prefer readable ids such as `node-api-gateway`, `node-order-service`, or `edge-user-gateway`.
- Keep labels short and readable.
- Escape XML-sensitive characters in labels and attributes:
  - `&` as `&amp;`
  - `<` as `&lt;`
  - `>` as `&gt;`
  - `"` as `&quot;`

## Edge Rules

- Every visible edge must be an `<mxCell>` with `edge="1"`.
- Every edge must include both `source="..."` and `target="..."`.
- Every edge must include `<mxGeometry relative="1" as="geometry" />`.
- Prefer orthogonal connectors:
  `edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;`
- Do not create edges that reference missing node ids.

## Layout Rules

- Prefer left-to-right layout for architecture diagrams.
- Prefer top-to-bottom layout for process diagrams.
- Avoid overlapping nodes.
- Use consistent spacing between rows and columns.
- Use group containers for major sections when they improve readability.
- Keep the diagram editable; do not generate one giant image node.

## Final Self-Check

Before returning the answer, verify:

- The response is valid JSON.
- The XML starts with `<mxfile` and ends with `</mxfile>`.
- The XML contains `<diagram>`, `<mxGraphModel>`, `<root>`, `id="0"`, and `id="1"`.
- Every node has geometry.
- Every edge has source, target, and relative geometry.
- No markdown fences or extra text are present.
