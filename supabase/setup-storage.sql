-- Run once in the Supabase SQL Editor.
-- The backend uses the service role key, so direct anon/authenticated object access
-- remains denied by default. Files are read through short-lived signed URLs only.
insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
)
values (
    'pawfit',
    'pawfit',
    false,
    10485760,
    array['image/jpeg', 'image/png', 'image/webp', 'application/pdf']
)
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

-- No permissive storage.objects policy is created intentionally.
-- The server-side service role bypasses RLS; mobile/web clients never receive it.
