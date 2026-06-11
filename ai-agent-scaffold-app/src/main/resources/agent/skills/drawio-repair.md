# drawio-repair

Use this skill when repairing invalid or low-quality Draw.io XML.

## Purpose

Repair the provided Draw.io response so it can be parsed as JSON, imported into draw.io, and edited normally.

The repaired answer must preserve the user's intended diagram as much as possible. Fix structure and formatting problems without inventing unrelated content.

## Input You May Receive

You may receive one or more of these inputs:

- Raw model output.
- Broken JSON.
- Broken or incomplete Draw.io XML.
- A harness diagnostic report.
- A list of validation issues.
- The original user request.

Use the diagnostics as the source of truth for what must be fixed.

## Output Contract

- Return JSON only.
- Do not return markdown fences.
- Do not add explanations, comments, or diagnostics.
- The response shape must be:
  `{"type":"drawio","content":"REPAIRED_FULL_MXFILE_XML"}`
- The `content` value must contain one complete `<mxfile>` document.
- If the input cannot be repaired into a meaningful diagram, return:
  `{"type":"user","content":"The diagram output could not be repaired. Please regenerate it from the original request."}`

## Repair Priorities

Fix issues in this order:

1. Remove markdown fences, prose, logs, partial events, or extra text outside JSON/XML.
2. Recover the intended `<mxfile>...</mxfile>` content.
3. Make the outer JSON valid.
4. Make the XML well-formed.
5. Restore required Draw.io structure.
6. Fix node geometry.
7. Fix edge source, target, and geometry.
8. Fix duplicate or missing ids.
9. Fix XML escaping.
10. Improve obvious layout problems only when the structural repair is complete.

## Required Draw.io Structure

The repaired XML must include:

- `<mxfile host="embed.diagrams.net" type="embed">`
- at least one `<diagram id="..." name="...">`
- one `<mxGraphModel ...>`
- one `<root>`
- `<mxCell id="0" />`
- `<mxCell id="1" parent="0" />`

If any of these are missing, add them.

## JSON Repair Rules

- If the raw output already contains usable XML, wrap it in:
  `{"type":"drawio","content":"..."}`
- Escape quotes inside the XML content when necessary for valid JSON.
- Preserve newlines only if they are valid JSON string content.
- Do not double-escape XML entities that are already correct.

## XML Repair Rules

- Ensure the XML starts with `<mxfile` and ends with `</mxfile>`.
- Close unclosed tags when the intended structure is clear.
- Remove malformed fragments that cannot be connected to the diagram.
- Add missing `parent="1"` to visible nodes when no valid parent exists.
- Add missing `vertex="1"` for visible node cells.
- Add missing `edge="1"` for connector cells.
- Add missing `<mxGeometry ... as="geometry" />` to nodes and edges.
- For edges, geometry must use `relative="1"`.
- Do not leave edges pointing to missing node ids.
- If an edge references a missing node and the intended target is unclear, remove that edge.

## Id Repair Rules

- Every `mxCell` id must be unique.
- Keep existing ids when they are valid and unique.
- Rename duplicates with readable suffixes such as `-2`, `-3`, or role-based ids.
- After renaming a node id, update all edge `source` and `target` references.
- Do not use spaces in ids.

## Geometry Repair Rules

- Nodes need numeric `x`, `y`, `width`, and `height`.
- Use reasonable fallback dimensions:
  - normal node: width `160`, height `64`
  - small label or note: width `180`, height `48`
  - group container: width `360`, height `220`
- Place fallback nodes on a simple grid if coordinates are missing.
- Keep nodes from overlapping when adding fallback coordinates.

## Edge Style Repair Rules

Prefer this style for normal directed edges:

`edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;`

Use `strokeColor=#667085` when no edge color is present.

## Escaping Rules

Escape XML-sensitive characters in attribute values:

- `&` as `&amp;`
- `<` as `&lt;`
- `>` as `&gt;`
- `"` as `&quot;`

Do not corrupt existing entities such as `&amp;`, `&lt;`, `&gt;`, or `&quot;`.

## Layout Repair Rules

Only make layout changes needed for readability or import success.

- Prefer left-to-right layout for architecture diagrams.
- Prefer top-to-bottom layout for process diagrams.
- Avoid node overlaps.
- Avoid long diagonal connectors.
- Keep group containers behind their child nodes.
- Keep labels horizontal.

## Final Self-Check

Before returning, verify:

- The response is valid JSON.
- There is no markdown or prose outside the JSON.
- `type` is either `drawio` or `user`.
- For `type="drawio"`, `content` contains a complete `<mxfile>`.
- The XML is well-formed.
- Required root cells exist.
- Every visible node has geometry.
- Every edge has source, target, and relative geometry.
- No edge references a missing node.
